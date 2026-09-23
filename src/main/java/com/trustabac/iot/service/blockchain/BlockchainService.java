package com.trustabac.iot.service.blockchain;

import com.trustabac.iot.blockchain.contract.AdaptiveAccessControl;
import com.trustabac.iot.config.BlockchainProperties;
import com.trustabac.iot.dto.BlockchainStatusResponse;
import com.trustabac.iot.dto.ContractThresholdResponse;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.exception.BlockchainUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthBlockNumber;
import org.web3j.protocol.core.methods.response.NetVersion;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
public class BlockchainService {

    private static final Logger log = LoggerFactory.getLogger(BlockchainService.class);

    private final Web3j web3j;
    private final Credentials credentials;
    private final ContractGasProvider gasProvider;
    private final BlockchainProperties properties;

    public BlockchainService(Web3j web3j, Credentials credentials,
                             ContractGasProvider gasProvider, BlockchainProperties properties) {
        this.web3j = web3j;
        this.credentials = credentials;
        this.gasProvider = gasProvider;
        this.properties = properties;
    }

    /**
     * Deploys a new instance of AdaptiveAccessControl smart contract to Ganache.
     */
    public synchronized String deployContract() {
        try {
            log.info("Deploying AdaptiveAccessControl contract to {}...", properties.getRpcUrl());
            AdaptiveAccessControl contract = AdaptiveAccessControl.deploy(
                    web3j, credentials, gasProvider
            ).send();
            String address = contract.getContractAddress();
            properties.setContractAddress(address);
            log.info("AdaptiveAccessControl contract deployed at address: {}", address);
            return address;
        } catch (Exception e) {
            log.error("Failed to deploy smart contract: {}", e.getMessage(), e);
            throw new BlockchainUnavailableException("Failed to deploy smart contract to blockchain: " + e.getMessage(), e);
        }
    }

    /**
     * Loads the smart contract at the configured address.
     */
    public AdaptiveAccessControl loadContract() {
        String address = properties.getContractAddress();
        if (address == null || address.isBlank()) {
            throw new BlockchainUnavailableException("Smart contract address is not configured");
        }
        return AdaptiveAccessControl.load(address, web3j, credentials, gasProvider);
    }

    /**
     * Queries the blockchain status and contract reachability.
     */
    public BlockchainStatusResponse getStatus() {
        boolean rpcReachable = false;
        Long chainId = null;
        Long latestBlock = null;
        boolean contractReachable = false;

        try {
            EthBlockNumber blockNumber = web3j.ethBlockNumber().send();
            if (blockNumber != null && blockNumber.getBlockNumber() != null) {
                rpcReachable = true;
                latestBlock = blockNumber.getBlockNumber().longValue();
            }

            NetVersion netVersion = web3j.netVersion().send();
            if (netVersion != null && netVersion.getNetVersion() != null) {
                try {
                    chainId = Long.parseLong(netVersion.getNetVersion());
                } catch (NumberFormatException ignored) {
                }
            }

            String address = properties.getContractAddress();
            if (rpcReachable && address != null && !address.isBlank()) {
                try {
                    AdaptiveAccessControl contract = loadContract();
                    String owner = contract.owner().send();
                    if (owner != null && !owner.isBlank()) {
                        contractReachable = true;
                    }
                } catch (Exception e) {
                    log.warn("Contract check failed at {}: {}", address, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Blockchain RPC status check failed for {}: {}", properties.getRpcUrl(), e.getMessage());
        }

        return new BlockchainStatusResponse(
                properties.getRpcUrl(),
                rpcReachable,
                chainId != null ? chainId : properties.getChainId(),
                latestBlock,
                properties.getContractAddress(),
                contractReachable,
                properties.isEnabled()
        );
    }

    /**
     * Queries the current configurable evaluation thresholds from the smart contract.
     */
    public ContractThresholdResponse getThresholds() {
        try {
            AdaptiveAccessControl contract = loadContract();
            BigInteger tHigh = contract.trustHigh().send();
            BigInteger tMed = contract.trustMedium().send();
            BigInteger rLow = contract.riskLow().send();
            BigInteger rMed = contract.riskMedium().send();

            return new ContractThresholdResponse(
                    tHigh.intValue(),
                    tMed.intValue(),
                    rLow.intValue(),
                    rMed.intValue(),
                    properties.getContractAddress()
            );
        } catch (Exception e) {
            log.error("Failed to query smart contract thresholds: {}", e.getMessage(), e);
            throw new BlockchainUnavailableException("Unable to read thresholds from smart contract: " + e.getMessage(), e);
        }
    }

    /**
     * Evaluates access by invoking the smart contract evaluateAccess transaction.
     * The decision is authoritatively produced by Solidity.
     */
    public BlockchainEvaluationResult evaluateAccessOnChain(
            String requestReference,
            String deviceIdentifier,
            boolean abacPass,
            boolean bookingActive,
            int resourceSensitivity,
            int operation,
            int trustScore,
            int riskScore
    ) {
        if (!properties.isEnabled()) {
            throw new BlockchainUnavailableException("Blockchain authorization engine is disabled");
        }

        try {
            AdaptiveAccessControl contract = loadContract();

            byte[] refBytes = toBytes32(requestReference);
            byte[] devBytes = Hash.sha3(deviceIdentifier.getBytes(StandardCharsets.UTF_8));

            BigInteger resSens = BigInteger.valueOf(resourceSensitivity);
            BigInteger op = BigInteger.valueOf(operation);
            BigInteger tScore = BigInteger.valueOf(trustScore);
            BigInteger rScore = BigInteger.valueOf(riskScore);

            log.info("Sending evaluateAccess transaction to smart contract at {}: abacPass={}, bookingActive={}, sens={}, op={}, trust={}, risk={}",
                    properties.getContractAddress(), abacPass, bookingActive, resourceSensitivity, operation, trustScore, riskScore);

            // Execute state-changing authorization transaction
            TransactionReceipt receipt = contract.evaluateAccess(
                    refBytes,
                    devBytes,
                    abacPass,
                    bookingActive,
                    resSens,
                    op,
                    tScore,
                    rScore
            ).send();

            if (!receipt.isStatusOK()) {
                throw new BlockchainUnavailableException("Blockchain transaction failed with status " + receipt.getStatus());
            }

            // Extract decision from emitted event or view call
            Decision decision;
            String reason;

            List<AdaptiveAccessControl.AuthorizationEvaluatedEventResponse> events =
                    AdaptiveAccessControl.getAuthorizationEvaluatedEvents(receipt);

            if (!events.isEmpty()) {
                AdaptiveAccessControl.AuthorizationEvaluatedEventResponse event = events.get(0);
                decision = Decision.fromSolidityCode(event.decision.intValue());
                reason = mapReasonCode(decision, abacPass, bookingActive, trustScore, riskScore);
            } else {
                // Fallback: query view calculation on contract
                Tuple2<BigInteger, BigInteger> viewRes = contract.calculateDecision(
                        abacPass, bookingActive, resSens, op, tScore, rScore
                ).send();
                decision = Decision.fromSolidityCode(viewRes.component1().intValue());
                reason = mapReasonCode(decision, abacPass, bookingActive, trustScore, riskScore);
            }

            String txHash = receipt.getTransactionHash();
            Long blockNum = receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null;

            log.info("Smart-contract evaluated access: decision={}, txHash={}, blockNumber={}", decision, txHash, blockNum);

            return new BlockchainEvaluationResult(
                    decision,
                    reason,
                    txHash,
                    blockNum,
                    properties.getContractAddress()
            );

        } catch (BlockchainUnavailableException bue) {
            throw bue;
        } catch (Exception e) {
            log.error("Blockchain execution failed: {}", e.getMessage(), e);
            throw new BlockchainUnavailableException("Smart contract authorization evaluation failed: " + e.getMessage(), e);
        }
    }

    private byte[] toBytes32(String input) {
        if (input == null || input.isBlank()) {
            return new byte[32];
        }
        byte[] raw = input.getBytes(StandardCharsets.UTF_8);
        if (raw.length == 32) {
            return raw;
        }
        if (raw.length > 32) {
            return Hash.sha3(raw);
        }
        byte[] padded = new byte[32];
        System.arraycopy(raw, 0, padded, 0, raw.length);
        return padded;
    }

    private String mapReasonCode(Decision decision, boolean abacPass, boolean bookingActive, int trustScore, int riskScore) {
        if (!abacPass) {
            return "Denied by Smart Contract: ABAC structural eligibility checks failed.";
        }
        if (!bookingActive) {
            return "Denied by Smart Contract: Booking reservation is inactive or expired.";
        }
        if (decision == Decision.ALLOW) {
            return String.format("Allowed by Smart Contract: High behavioral trust (%d) and low contextual risk (%d).", trustScore, riskScore);
        }
        if (decision == Decision.RESTRICT) {
            return String.format("Restricted by Smart Contract: Adaptive authorization enforced for intermediate trust (%d) or moderate risk (%d).", trustScore, riskScore);
        }
        return String.format("Denied by Smart Contract: Insufficient trust (%d) or excessive contextual risk (%d).", trustScore, riskScore);
    }

    public record BlockchainEvaluationResult(
            Decision decision,
            String reason,
            String transactionHash,
            Long blockNumber,
            String contractAddress
    ) {
    }
}
