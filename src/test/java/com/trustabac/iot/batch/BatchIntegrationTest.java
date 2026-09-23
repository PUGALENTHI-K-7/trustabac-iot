package com.trustabac.iot.batch;

import com.trustabac.iot.batch.dto.BatchRunRequest;
import com.trustabac.iot.batch.dto.BatchRunResponse;
import com.trustabac.iot.batch.entity.AuthorizationAnalytics;
import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.repository.AuthorizationAnalyticsRepository;
import com.trustabac.iot.batch.repository.BatchRunAuditRepository;
import com.trustabac.iot.batch.repository.DeviceAnalyticsRepository;
import com.trustabac.iot.batch.repository.SecurityAnalyticsRepository;
import com.trustabac.iot.batch.service.BatchAuditService;
import com.trustabac.iot.entity.*;
import com.trustabac.iot.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BatchIntegrationTest {

    @Autowired
    private BatchAuditService batchAuditService;

    @Autowired
    private BatchRunAuditRepository batchRunAuditRepository;

    @Autowired
    private AuthorizationAnalyticsRepository authorizationAnalyticsRepository;

    @Autowired
    private DeviceAnalyticsRepository deviceAnalyticsRepository;

    @Autowired
    private SecurityAnalyticsRepository securityAnalyticsRepository;

    @Autowired
    private AccessRequestRepository accessRequestRepository;

    @Autowired
    private BlockchainAuthorizationEventRepository blockchainRepository;

    @Autowired
    private TrustHistoryRepository trustHistoryRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Test
    @DisplayName("End-to-End Batch Integration: Process persisted history, assert analytics, verify idempotency and non-mutation")
    void testEndToEndBatchAnalytics() {
        // 1. Seed operational data
        Device dev = deviceRepository.save(new Device("DEV-BATCH-INTEG-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true));

        AccessRequest req = accessRequestRepository.save(new AccessRequest(
                "DEV-BATCH-INTEG-001", "user-1", "GUEST", "ORG-1", "SMART_DOOR_LOCK", "ACTUATOR",
                Operation.CONTROL, "PROP-1", "BOOK-1", "INTERNAL", LocalDateTime.now(),
                AbacResult.PASS, "OK", "Policy1"
        ));

        BlockchainAuthorizationEvent bc = blockchainRepository.save(new BlockchainAuthorizationEvent(
                "DEV-BATCH-INTEG-001", "SMART_DOOR_LOCK", "CONTROL", 80.0, 14.0, Decision.ALLOW,
                "OK", "0xContract", "0xIntegTxHash123", 500L, LocalDateTime.now()
        ));

        TrustHistory th = trustHistoryRepository.save(new TrustHistory(
                dev, "DEV-BATCH-INTEG-001", 80.0, 81.0, 1.0, TrustEventType.NORMAL_SUCCESS,
                "Normal", "SYS", LocalDateTime.now()
        ));

        RiskEvent re = riskEventRepository.save(new RiskEvent(
                dev, "DEV-BATCH-INTEG-001", 14.0, RiskStatus.LOW, "SMART_DOOR_LOCK",
                Operation.CONTROL, "factors", "reason", LocalDateTime.now()
        ));

        String periodKey = "INTEG_PERIOD_" + System.currentTimeMillis();

        // Record initial state of operational records
        double initialTrust = dev.getCurrentTrust();
        long initialAccessReqCount = accessRequestRepository.count();
        long initialBcCount = blockchainRepository.count();
        long initialTrustHistCount = trustHistoryRepository.count();
        long initialRiskEventCount = riskEventRepository.count();

        // 2. Execute initial Batch run
        BatchRunRequest request = new BatchRunRequest(periodKey, null, null, false);
        BatchRunResponse firstResponse = batchAuditService.runAudit(request);

        assertNotNull(firstResponse);
        assertFalse(firstResponse.isAlreadyProcessed());
        assertEquals("COMPLETED", firstResponse.getStatus());

        // 3. Verify Batch outputs in analytical tables
        BatchRunAudit audit = batchRunAuditRepository.findByPeriodKey(periodKey).orElse(null);
        assertNotNull(audit, "BatchRunAudit record must be created");
        assertEquals("COMPLETED", audit.getStatus());
        assertTrue(audit.getTotalRecordsRead() > 0);

        AuthorizationAnalytics auth = authorizationAnalyticsRepository.findByPeriodKey(periodKey).orElse(null);
        assertNotNull(auth, "AuthorizationAnalytics must be created");
        assertTrue(auth.getAllowCount() >= 1);
        assertEquals(0, auth.getMissingBlockchainProofs());
        assertTrue(auth.getMatchedBlockchainProofs() >= 1);

        assertFalse(deviceAnalyticsRepository.findAllByPeriodKey(periodKey).isEmpty(), "DeviceAnalytics must be created");
        assertTrue(securityAnalyticsRepository.findByPeriodKey(periodKey).isPresent(), "SecurityAnalytics must be created");

        // 4. Verify Idempotency: Running the same period again must NOT duplicate records
        long initialAuditCount = batchRunAuditRepository.count();
        long initialAuthAnalyticsCount = authorizationAnalyticsRepository.count();

        BatchRunResponse secondResponse = batchAuditService.runAudit(request);
        assertTrue(secondResponse.isAlreadyProcessed(), "Second run must be marked alreadyProcessed");
        assertEquals(firstResponse.getBatchRunId(), secondResponse.getBatchRunId());

        assertEquals(initialAuditCount, batchRunAuditRepository.count(), "Audit records must not duplicate");
        assertEquals(initialAuthAnalyticsCount, authorizationAnalyticsRepository.count(), "Analytics records must not duplicate");

        // 5. Verify Operational Non-Mutation: Operational tables must remain 100% untouched
        Device postRunDev = deviceRepository.findById(dev.getId()).orElseThrow();
        assertEquals(initialTrust, postRunDev.getCurrentTrust(), "Device current trust must NOT be mutated by batch processing");
        assertEquals(initialAccessReqCount, accessRequestRepository.count(), "Access requests count must not change");
        assertEquals(initialBcCount, blockchainRepository.count(), "Blockchain events count must not change");
        assertEquals(initialTrustHistCount, trustHistoryRepository.count(), "Trust history count must not change");
        assertEquals(initialRiskEventCount, riskEventRepository.count(), "Risk events count must not change");
    }
}
