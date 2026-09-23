package com.trustabac.iot.experiment.service;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * High-precision statistics calculator for experiment latencies, gas, and throughput metrics.
 * Implements percentile computations (p25, p50/median, p75, p95, p99), IQR, standard deviation,
 * standard error, and Student's t-distribution based 95% confidence intervals.
 */
@Component
public class ExperimentStatisticsCalculator {

    public static class LatencyStats {
        private final int sampleSize;
        private final Double minMs;
        private final Double maxMs;
        private final Double meanMs;
        private final Double medianMs;
        private final Double p95Ms;
        private final Double p99Ms;
        private final Double stdDevMs;
        private final Double stdErrorMs;
        private final Double iqrMs;
        private final Double ci95LowerMs;
        private final Double ci95UpperMs;
        private final String ciMethod;
        private final String sampleLimitationNotice;

        public LatencyStats(int sampleSize, Double minMs, Double maxMs, Double meanMs, Double medianMs,
                            Double p95Ms, Double p99Ms, Double stdDevMs, Double stdErrorMs, Double iqrMs,
                            Double ci95LowerMs, Double ci95UpperMs, String ciMethod, String sampleLimitationNotice) {
            this.sampleSize = sampleSize;
            this.minMs = minMs;
            this.maxMs = maxMs;
            this.meanMs = meanMs;
            this.medianMs = medianMs;
            this.p95Ms = p95Ms;
            this.p99Ms = p99Ms;
            this.stdDevMs = stdDevMs;
            this.stdErrorMs = stdErrorMs;
            this.iqrMs = iqrMs;
            this.ci95LowerMs = ci95LowerMs;
            this.ci95UpperMs = ci95UpperMs;
            this.ciMethod = ciMethod;
            this.sampleLimitationNotice = sampleLimitationNotice;
        }

        public int getSampleSize() {
            return sampleSize;
        }

        public Double getMinMs() {
            return minMs;
        }

        public Double getMaxMs() {
            return maxMs;
        }

        public Double getMeanMs() {
            return meanMs;
        }

        public Double getMedianMs() {
            return medianMs;
        }

        public Double getP95Ms() {
            return p95Ms;
        }

        public Double getP99Ms() {
            return p99Ms;
        }

        public Double getStdDevMs() {
            return stdDevMs;
        }

        public Double getStdErrorMs() {
            return stdErrorMs;
        }

        public Double getIqrMs() {
            return iqrMs;
        }

        public Double getCi95LowerMs() {
            return ci95LowerMs;
        }

        public Double getCi95UpperMs() {
            return ci95UpperMs;
        }

        public String getCiMethod() {
            return ciMethod;
        }

        public String getSampleLimitationNotice() {
            return sampleLimitationNotice;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sampleSize", sampleSize);
            m.put("minMs", minMs);
            m.put("maxMs", maxMs);
            m.put("meanMs", meanMs);
            m.put("medianMs", medianMs);
            m.put("p95Ms", p95Ms);
            m.put("p99Ms", p99Ms);
            m.put("stdDevMs", stdDevMs);
            m.put("stdErrorMs", stdErrorMs);
            m.put("iqrMs", iqrMs);
            m.put("ci95LowerMs", ci95LowerMs);
            m.put("ci95UpperMs", ci95UpperMs);
            m.put("ciMethod", ciMethod);
            if (sampleLimitationNotice != null) {
                m.put("limitationNotice", sampleLimitationNotice);
            }
            return m;
        }
    }

    public static class GasStats {
        private final int transactionCount;
        private final Long totalGas;
        private final Double meanGas;
        private final Long minGas;
        private final Long maxGas;
        private final Double medianGas;
        private final Double p95Gas;

        public GasStats(int transactionCount, Long totalGas, Double meanGas, Long minGas, Long maxGas, Double medianGas, Double p95Gas) {
            this.transactionCount = transactionCount;
            this.totalGas = totalGas;
            this.meanGas = meanGas;
            this.minGas = minGas;
            this.maxGas = maxGas;
            this.medianGas = medianGas;
            this.p95Gas = p95Gas;
        }

        public int getTransactionCount() { return transactionCount; }
        public Long getTotalGas() { return totalGas; }
        public Double getMeanGas() { return meanGas; }
        public Long getMinGas() { return minGas; }
        public Long getMaxGas() { return maxGas; }
        public Double getMedianGas() { return medianGas; }
        public Double getP95Gas() { return p95Gas; }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("transactionCount", transactionCount);
            m.put("totalGasUsed", totalGas);
            m.put("meanGas", meanGas);
            m.put("minGas", minGas);
            m.put("maxGas", maxGas);
            m.put("medianGas", medianGas);
            m.put("p95Gas", p95Gas);
            return m;
        }
    }

    public LatencyStats calculateStats(List<Double> latencyValuesMs) {
        if (latencyValuesMs == null || latencyValuesMs.isEmpty()) {
            return new LatencyStats(0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, "NONE", "Sample size is 0");
        }

        List<Double> sorted = new ArrayList<>(latencyValuesMs);
        Collections.sort(sorted);
        int n = sorted.size();

        double min = sorted.get(0);
        double max = sorted.get(n - 1);
        double sum = 0.0;
        for (double val : sorted) {
            sum += val;
        }
        double mean = round(sum / n, 3);
        double p25 = getPercentile(sorted, 25.0);
        double median = round(getPercentile(sorted, 50.0), 3);
        double p75 = getPercentile(sorted, 75.0);
        double p95 = round(getPercentile(sorted, 95.0), 3);
        double p99 = round(getPercentile(sorted, 99.0), 3);
        double iqr = round(p75 - p25, 3);

        // Standard deviation and Standard error
        double varianceSum = 0.0;
        for (double val : sorted) {
            varianceSum += Math.pow(val - mean, 2);
        }
        double stdDev = n > 1 ? round(Math.sqrt(varianceSum / (n - 1)), 3) : 0.0;
        double stdError = n > 0 ? round(stdDev / Math.sqrt(n), 3) : 0.0;

        // 95% Confidence Interval using Student's t critical value
        double tCrit = getStudentTCriticalValue(n - 1);
        double marginOfError = tCrit * stdError;
        double ci95Lower = round(Math.max(0.0, mean - marginOfError), 3);
        double ci95Upper = round(mean + marginOfError, 3);
        String ciMethod = n >= 120 ? "STUDENT_T_LARGE_SAMPLE_APPROX (df=" + (n - 1) + ", t=1.960)"
                : n > 1 ? "STUDENT_T (df=" + (n - 1) + ", t=" + round(tCrit, 3) + ")"
                : "INSUFFICIENT_SAMPLES (N=1)";

        String notice = null;
        if (n < 20) {
            notice = "Exploratory sample (N=" + n + " < 20). Percentiles and confidence intervals have wider margins of error.";
        } else if (n < 100) {
            notice = "Intermediate sample size (N=" + n + "). Sufficient for scenario verification, bounded variance.";
        }

        return new LatencyStats(n, min, max, mean, median, p95, p99, stdDev, stdError, iqr, ci95Lower, ci95Upper, ciMethod, notice);
    }

    public GasStats calculateGasStats(List<Long> gasValues) {
        if (gasValues == null || gasValues.isEmpty()) {
            return new GasStats(0, 0L, 0.0, 0L, 0L, 0.0, 0.0);
        }

        List<Long> sorted = new ArrayList<>(gasValues);
        Collections.sort(sorted);
        int n = sorted.size();

        long min = sorted.get(0);
        long max = sorted.get(n - 1);
        long sum = 0L;
        for (long g : sorted) {
            sum += g;
        }
        double mean = round((double) sum / n, 2);

        List<Double> doubleList = new ArrayList<>(n);
        for (Long g : sorted) doubleList.add((double) g);

        double median = round(getPercentile(doubleList, 50.0), 2);
        double p95 = round(getPercentile(doubleList, 95.0), 2);

        return new GasStats(n, sum, mean, min, max, median, p95);
    }

    public double calculateThroughput(int operations, long durationMs) {
        if (durationMs <= 0 || operations <= 0) {
            return 0.0;
        }
        return round(((double) operations / durationMs) * 1000.0, 2);
    }

    private double getPercentile(List<Double> sorted, double percentile) {
        int n = sorted.size();
        if (n == 1) {
            return sorted.get(0);
        }
        double rank = (percentile / 100.0) * (n - 1);
        int lower = (int) Math.floor(rank);
        int upper = (int) Math.ceil(rank);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double weight = rank - lower;
        return sorted.get(lower) * (1.0 - weight) + sorted.get(upper) * weight;
    }

    /**
     * Standard two-tailed Student's t critical values for alpha=0.05 (95% confidence level).
     */
    public double getStudentTCriticalValue(int df) {
        if (df <= 0) return 1.960;
        switch (df) {
            case 1:  return 12.706;
            case 2:  return 4.303;
            case 3:  return 3.182;
            case 4:  return 2.776;
            case 5:  return 2.571;
            case 6:  return 2.447;
            case 7:  return 2.365;
            case 8:  return 2.306;
            case 9:  return 2.262;
            case 10: return 2.228;
            case 11: return 2.201;
            case 12: return 2.179;
            case 13: return 2.160;
            case 14: return 2.145;
            case 15: return 2.131;
            case 16: return 2.120;
            case 17: return 2.110;
            case 18: return 2.101;
            case 19: return 2.093;
            case 20: return 2.086;
            case 25: return 2.060;
            case 30: return 2.042;
            case 40: return 2.021;
            case 50: return 2.009;
            case 60: return 2.000;
            case 80: return 1.990;
            case 99: return 1.984;
            case 100: return 1.984;
            case 120: return 1.980;
            default:
                if (df < 25) return 2.086 - (df - 20) * (0.026 / 5.0);
                if (df < 30) return 2.060 - (df - 25) * (0.018 / 5.0);
                if (df < 40) return 2.042 - (df - 30) * (0.021 / 10.0);
                if (df < 60) return 2.021 - (df - 40) * (0.021 / 20.0);
                if (df < 120) return 2.000 - (df - 60) * (0.020 / 60.0);
                return 1.960; // Standard normal z-critical asymptote for large sample sizes
        }
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
