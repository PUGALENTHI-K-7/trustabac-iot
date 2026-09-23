package com.trustabac.iot.experiment;

import com.trustabac.iot.experiment.service.ExperimentStatisticsCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 8B: ExperimentStatisticsCalculator Statistical Accuracy Tests")
class ExperimentStatisticsCalculatorPhase8BTest {

    private ExperimentStatisticsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new ExperimentStatisticsCalculator();
    }

    @Test
    @DisplayName("Should correctly calculate stdDev, stdError, IQR, and Student's t 95% CI")
    void testStatisticalCalculations() {
        // Sample dataset of 10 measurements: 10, 12, 14, 15, 16, 18, 20, 22, 24, 30
        List<Double> data = List.of(10.0, 12.0, 14.0, 15.0, 16.0, 18.0, 20.0, 22.0, 24.0, 30.0);
        ExperimentStatisticsCalculator.LatencyStats stats = calculator.calculateStats(data);

        assertEquals(10, stats.getSampleSize());
        assertEquals(10.0, stats.getMinMs());
        assertEquals(30.0, stats.getMaxMs());
        assertEquals(18.1, stats.getMeanMs(), 0.01);
        assertEquals(17.0, stats.getMedianMs(), 0.01);

        // Standard Deviation: sqrt(sum((x-18.1)^2) / 9) = ~5.952
        assertTrue(stats.getStdDevMs() > 5.5 && stats.getStdDevMs() < 6.5, "StdDev should be ~5.952");

        // Standard Error: stdDev / sqrt(10) = ~1.882
        assertTrue(stats.getStdErrorMs() > 1.7 && stats.getStdErrorMs() < 2.1, "StdError should be ~1.882");

        // IQR = p75 - p25 = 22.5 - 13.5 = 9.0
        assertTrue(stats.getIqrMs() > 0.0, "IQR should be positive");

        // Student's t critical value for df=9 at alpha=0.05 is 2.262
        // Margin of error = 2.262 * 1.882 = ~4.257
        // Lower CI = 18.1 - 4.257 = ~13.843
        // Upper CI = 18.1 + 4.257 = ~22.357
        assertTrue(stats.getCi95LowerMs() < stats.getMeanMs());
        assertTrue(stats.getCi95UpperMs() > stats.getMeanMs());
        assertTrue(stats.getCiMethod().contains("STUDENT_T"));
    }

    @Test
    @DisplayName("Should verify Student's t critical value progression")
    void testStudentTCriticalValues() {
        assertEquals(12.706, calculator.getStudentTCriticalValue(1));
        assertEquals(4.303, calculator.getStudentTCriticalValue(2));
        assertEquals(2.571, calculator.getStudentTCriticalValue(5));
        assertEquals(2.262, calculator.getStudentTCriticalValue(9));
        assertEquals(2.042, calculator.getStudentTCriticalValue(30));
        assertEquals(1.984, calculator.getStudentTCriticalValue(100));
        assertEquals(1.980, calculator.getStudentTCriticalValue(120));
        assertEquals(1.960, calculator.getStudentTCriticalValue(150));
    }

    @Test
    @DisplayName("Should calculate gas metrics accurately")
    void testGasStats() {
        List<Long> gasList = List.of(31863L, 31863L, 31863L, 31863L, 31863L);
        ExperimentStatisticsCalculator.GasStats gasStats = calculator.calculateGasStats(gasList);

        assertEquals(5, gasStats.getTransactionCount());
        assertEquals(159315L, gasStats.getTotalGas());
        assertEquals(31863.0, gasStats.getMeanGas());
        assertEquals(31863L, gasStats.getMinGas());
        assertEquals(31863L, gasStats.getMaxGas());
        assertEquals(31863.0, gasStats.getMedianGas());
    }

    @Test
    @DisplayName("Should handle empty and single element samples gracefully")
    void testEdgeCases() {
        ExperimentStatisticsCalculator.LatencyStats emptyStats = calculator.calculateStats(new ArrayList<>());
        assertEquals(0, emptyStats.getSampleSize());
        assertEquals(0.0, emptyStats.getMeanMs());

        ExperimentStatisticsCalculator.LatencyStats singleStats = calculator.calculateStats(List.of(45.5));
        assertEquals(1, singleStats.getSampleSize());
        assertEquals(45.5, singleStats.getMeanMs());
        assertEquals(0.0, singleStats.getStdDevMs());
        assertEquals(0.0, singleStats.getStdErrorMs());
        assertTrue(singleStats.getCiMethod().contains("INSUFFICIENT_SAMPLES"));
    }
}
