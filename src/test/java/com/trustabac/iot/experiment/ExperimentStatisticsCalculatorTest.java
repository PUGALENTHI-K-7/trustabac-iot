package com.trustabac.iot.experiment;

import com.trustabac.iot.experiment.service.ExperimentStatisticsCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExperimentStatisticsCalculatorTest {

    private final ExperimentStatisticsCalculator calculator = new ExperimentStatisticsCalculator();

    @Test
    @DisplayName("Verify statistical calculations: min, max, mean, median, p95, p99, and sample notice")
    void testStatisticalCalculations() {
        List<Double> sample = List.of(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0);
        ExperimentStatisticsCalculator.LatencyStats stats = calculator.calculateStats(sample);

        assertEquals(10, stats.getSampleSize());
        assertEquals(10.0, stats.getMinMs());
        assertEquals(100.0, stats.getMaxMs());
        assertEquals(55.0, stats.getMeanMs());
        assertEquals(55.0, stats.getMedianMs());
        assertTrue(stats.getP95Ms() > 90.0);
        assertTrue(stats.getP99Ms() > 95.0);
        assertNotNull(stats.getSampleLimitationNotice(), "Sample size N=10 must include limitation notice");
    }

    @Test
    @DisplayName("Verify throughput calculation")
    void testThroughputCalculation() {
        double throughput = calculator.calculateThroughput(50, 2000L);
        assertEquals(25.0, throughput);

        double zeroThroughput = calculator.calculateThroughput(0, 2000L);
        assertEquals(0.0, zeroThroughput);
    }
}
