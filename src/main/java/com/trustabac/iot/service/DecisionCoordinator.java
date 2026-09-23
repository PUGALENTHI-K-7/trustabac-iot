package com.trustabac.iot.service;

import com.trustabac.iot.dto.AccessEvaluationRequest;
import com.trustabac.iot.dto.AccessEvaluationResponse;
import com.trustabac.iot.dto.BlockchainAuthorizationRequest;
import com.trustabac.iot.dto.BlockchainAuthorizationResponse;
import com.trustabac.iot.entity.AbacResult;
import com.trustabac.iot.entity.BlockchainAuthorizationEvent;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.ResourceSensitivity;
import com.trustabac.iot.exception.BlockchainUnavailableException;
import com.trustabac.iot.repository.BlockchainAuthorizationEventRepository;
import com.trustabac.iot.service.blockchain.BlockchainService;
import com.trustabac.iot.service.risk.ResourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DecisionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(DecisionCoordinator.class);

    private final AbacService abacService;
    private final ResourceRegistry resourceRegistry;
    private final BlockchainService blockchainService;
    private final BlockchainAuthorizationEventRepository authorizationEventRepository;
    private final Clock clock;

    public DecisionCoordinator(AbacService abacService,
                               ResourceRegistry resourceRegistry,
                               BlockchainService blockchainService,
                               BlockchainAuthorizationEventRepository authorizationEventRepository,
                               Clock clock) {
        this.abacService = abacService;
        this.resourceRegistry = resourceRegistry;
        this.blockchainService = blockchainService;
        this.authorizationEventRepository = authorizationEventRepository;
        this.clock = clock;
    }

    @Transactional
    public BlockchainAuthorizationResponse evaluateAuthorization(BlockchainAuthorizationRequest request) {
        LocalDateTime evaluationTimestamp = LocalDateTime.now(clock);
        String requestRef = request.requestReference() != null && !request.requestReference().isBlank()
                ? request.requestReference()
                : UUID.randomUUID().toString();

        // 1. Convert to AccessEvaluationRequest and evaluate ABAC eligibility (Gate 1)
        AccessEvaluationRequest abacReq = new AccessEvaluationRequest();
        abacReq.setDeviceIdentifier(request.deviceIdentifier());
        abacReq.setUserId(request.userId());
        abacReq.setRole(request.role());
        abacReq.setOrganization(request.organization());
        abacReq.setResource(request.resource());
        try {
            abacReq.setOperation(Operation.valueOf(request.operation().toUpperCase()));
        } catch (Exception e) {
            abacReq.setOperation(Operation.READ);
        }
        abacReq.setLocation(request.location());
        abacReq.setBookingId(request.bookingId());
        abacReq.setNetworkContext(request.networkContext());
        abacReq.setRequestCountWindow(request.requestCountWindow());
        abacReq.setRecentViolationCount(request.recentViolationCount());
        if (request.behavioralIndicator() != null && !request.behavioralIndicator().isBlank()) {
            try {
                abacReq.setBehavioralIndicator(com.trustabac.iot.entity.BehavioralIndicator.valueOf(request.behavioralIndicator().toUpperCase()));
            } catch (Exception ignored) {
            }
        }

        AccessEvaluationResponse abacRes = abacService.evaluateAccess(abacReq);

        // 2. Hard Gate: ABAC FAIL -> Stop immediately, no Risk evaluation, no Trust mutation, no smart contract
        if (abacRes.getResult() != AbacResult.PASS) {
            log.info("Authorization DENIED at ABAC Gate: device='{}', user='{}', reason='{}'",
                    request.deviceIdentifier(), request.userId(), abacRes.getReason());

            return new BlockchainAuthorizationResponse(
                    abacRes.getRequestId(),
                    request.deviceIdentifier(),
                    request.userId(),
                    request.resource(),
                    request.operation(),
                    "FAIL",
                    abacRes.getReason(),
                    abacRes.getEvaluatedPolicyName(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Decision.DENY,
                    "Access Denied: ABAC eligibility failed - " + abacRes.getReason(),
                    null,
                    null,
                    null,
                    evaluationTimestamp
            );
        }

        // 3. ABAC PASS: Trust and Risk signals are already evaluated and attached by AbacService
        Double trustScore = abacRes.getTrustScore() != null ? abacRes.getTrustScore() : 80.0;
        String trustStatus = abacRes.getTrustStatus();
        Double riskScore = abacRes.getRiskScore() != null ? abacRes.getRiskScore() : 0.0;
        String riskStatus = abacRes.getRiskStatus() != null ? abacRes.getRiskStatus().name() : "LOW";

        // 4. Resolve authoritative server-side context for Smart Contract
        ResourceSensitivity sensitivity = resourceRegistry.resolveSensitivity(request.resource());
        int sensitivityCode = mapSensitivityCode(sensitivity);
        int operationCode = mapOperationCode(request.operation());

        boolean bookingActive = isBookingActive(request.bookingId(), abacRes);

        int compactTrust = (int) Math.round(trustScore);
        int compactRisk = (int) Math.round(riskScore);

        // 5. Invoke Authoritative Smart Contract via BlockchainService
        Decision finalDecision;
        String decisionReason;
        String txHash = null;
        Long blockNumber = null;
        String contractAddress = null;

        try {
            BlockchainService.BlockchainEvaluationResult result = blockchainService.evaluateAccessOnChain(
                    requestRef,
                    request.deviceIdentifier(),
                    true,
                    bookingActive,
                    sensitivityCode,
                    operationCode,
                    compactTrust,
                    compactRisk
            );

            finalDecision = result.decision();
            decisionReason = result.reason();
            txHash = result.transactionHash();
            blockNumber = result.blockNumber();
            contractAddress = result.contractAddress();

            // 6. Persist Off-Chain Audit Log
            BlockchainAuthorizationEvent auditEvent = new BlockchainAuthorizationEvent(
                    request.deviceIdentifier(),
                    request.resource(),
                    request.operation(),
                    trustScore,
                    riskScore,
                    finalDecision,
                    decisionReason,
                    contractAddress,
                    txHash,
                    blockNumber,
                    evaluationTimestamp
            );
            authorizationEventRepository.save(auditEvent);

        } catch (BlockchainUnavailableException bue) {
            // Fail-Closed Enforcement: Never allow access if blockchain is unavailable
            log.error("Blockchain authorization unavailable: {}. Enforcing Fail-Closed DENY.", bue.getMessage());
            finalDecision = Decision.DENY;
            decisionReason = "Access Denied: Blockchain authorization engine unavailable (Fail-Closed Security Enforcement).";
        }

        return new BlockchainAuthorizationResponse(
                abacRes.getRequestId(),
                request.deviceIdentifier(),
                request.userId(),
                request.resource(),
                request.operation(),
                "PASS",
                abacRes.getReason(),
                abacRes.getEvaluatedPolicyName(),
                trustScore,
                trustStatus,
                riskScore,
                riskStatus,
                abacRes.getRiskFactors(),
                abacRes.getRiskReason(),
                finalDecision,
                decisionReason,
                txHash,
                blockNumber,
                contractAddress,
                evaluationTimestamp
        );
    }

    private boolean isBookingActive(String bookingId, AccessEvaluationResponse abacRes) {
        if (bookingId == null || bookingId.isBlank()) {
            return false;
        }
        return abacRes.getReason() == null || !abacRes.getReason().toLowerCase().contains("booking");
    }

    private int mapSensitivityCode(ResourceSensitivity sensitivity) {
        if (sensitivity == null) {
            return 1; // MEDIUM
        }
        return switch (sensitivity) {
            case LOW -> 0;
            case MEDIUM -> 1;
            case HIGH -> 2;
            case CRITICAL -> 3;
        };
    }

    private int mapOperationCode(String operation) {
        if (operation == null) {
            return 0;
        }
        return switch (operation.toUpperCase()) {
            case "READ" -> 0;
            case "WRITE" -> 1;
            case "UPDATE" -> 2;
            case "DELETE" -> 3;
            case "CONTROL" -> 4;
            default -> 0;
        };
    }
}
