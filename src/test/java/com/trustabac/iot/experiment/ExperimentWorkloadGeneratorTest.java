package com.trustabac.iot.experiment;

import com.trustabac.iot.dto.ResourceOperationRequest;
import com.trustabac.iot.experiment.dto.ScenarioType;
import com.trustabac.iot.experiment.service.ExperimentWorkloadGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentWorkloadGeneratorTest {

    private final ExperimentWorkloadGenerator generator = new ExperimentWorkloadGenerator();

    @Test
    @DisplayName("Verify deterministic workload generation with identical seeds")
    void testDeterministicSeedReproducibility() {
        List<ResourceOperationRequest> run1 = generator.generateWorkload(ScenarioType.NORMAL_ACCESS, 10, 42L);
        List<ResourceOperationRequest> run2 = generator.generateWorkload(ScenarioType.NORMAL_ACCESS, 10, 42L);

        assertEquals(10, run1.size());
        assertEquals(10, run2.size());

        for (int i = 0; i < 10; i++) {
            assertEquals(run1.get(i).deviceIdentifier(), run2.get(i).deviceIdentifier());
            assertEquals(run1.get(i).operation(), run2.get(i).operation());
            assertEquals(run1.get(i).resource(), run2.get(i).resource());
        }
    }

    @Test
    @DisplayName("Verify workload generation for all 8 experimental scenarios")
    void testAllScenariosGeneration() {
        for (ScenarioType scenario : ScenarioType.values()) {
            List<ResourceOperationRequest> workload = generator.generateWorkload(scenario, 5, 123L);
            assertNotNull(workload);
            assertEquals(5, workload.size());
        }
    }
}
