package com.trustabac.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration properties for the Contextual Risk Engine.
 * Note: Configurable prototype parameters for empirical evaluation;
 * not claimed as universal constants.
 */
@Configuration
@ConfigurationProperties(prefix = "trustabac.risk")
public class RiskProperties {

    private double minimumScore = 0.0;
    private double maximumScore = 100.0;
    private double lowThreshold = 30.0;
    private double mediumThreshold = 70.0;

    private Weights weights = new Weights();
    private Context context = new Context();

    public static class Weights {
        private double time = 10.0;
        private double location = 15.0;
        private double sensitivity = 20.0;
        private double frequency = 20.0;
        private double network = 15.0;
        private double violations = 10.0;
        private double behavior = 10.0;

        public double getTime() {
            return time;
        }

        public void setTime(double time) {
            this.time = time;
        }

        public double getLocation() {
            return location;
        }

        public void setLocation(double location) {
            this.location = location;
        }

        public double getSensitivity() {
            return sensitivity;
        }

        public void setSensitivity(double sensitivity) {
            this.sensitivity = sensitivity;
        }

        public double getFrequency() {
            return frequency;
        }

        public void setFrequency(double frequency) {
            this.frequency = frequency;
        }

        public double getNetwork() {
            return network;
        }

        public void setNetwork(double network) {
            this.network = network;
        }

        public double getViolations() {
            return violations;
        }

        public void setViolations(double violations) {
            this.violations = violations;
        }

        public double getBehavior() {
            return behavior;
        }

        public void setBehavior(double behavior) {
            this.behavior = behavior;
        }

        public double getTotalWeight() {
            return time + location + sensitivity + frequency + network + violations + behavior;
        }
    }

    public static class Context {
        private int normalStartHour = 6;
        private int normalEndHour = 22;
        private String defaultExpectedLocation = "Property-001";
        private int frequencyLowThreshold = 5;
        private int frequencyHighThreshold = 20;
        private int violationLowThreshold = 1;
        private int violationHighThreshold = 5;

        private double networkLocalWifi = 0.0;
        private double networkVpn = 0.30;
        private double networkRemoteCellular = 0.60;
        private double networkUnknown = 1.0;

        private double sensitivityLow = 0.0;
        private double sensitivityMedium = 0.35;
        private double sensitivityHigh = 0.70;
        private double sensitivityCritical = 1.0;

        private double behaviorNormal = 0.0;
        private double behaviorSuspicious = 0.60;
        private double behaviorAbnormal = 1.0;

        public int getNormalStartHour() {
            return normalStartHour;
        }

        public void setNormalStartHour(int normalStartHour) {
            this.normalStartHour = normalStartHour;
        }

        public int getNormalEndHour() {
            return normalEndHour;
        }

        public void setNormalEndHour(int normalEndHour) {
            this.normalEndHour = normalEndHour;
        }

        public String getDefaultExpectedLocation() {
            return defaultExpectedLocation;
        }

        public void setDefaultExpectedLocation(String defaultExpectedLocation) {
            this.defaultExpectedLocation = defaultExpectedLocation;
        }

        public int getFrequencyLowThreshold() {
            return frequencyLowThreshold;
        }

        public void setFrequencyLowThreshold(int frequencyLowThreshold) {
            this.frequencyLowThreshold = frequencyLowThreshold;
        }

        public int getFrequencyHighThreshold() {
            return frequencyHighThreshold;
        }

        public void setFrequencyHighThreshold(int frequencyHighThreshold) {
            this.frequencyHighThreshold = frequencyHighThreshold;
        }

        public int getViolationLowThreshold() {
            return violationLowThreshold;
        }

        public void setViolationLowThreshold(int violationLowThreshold) {
            this.violationLowThreshold = violationLowThreshold;
        }

        public int getViolationHighThreshold() {
            return violationHighThreshold;
        }

        public void setViolationHighThreshold(int violationHighThreshold) {
            this.violationHighThreshold = violationHighThreshold;
        }

        public double getNetworkLocalWifi() {
            return networkLocalWifi;
        }

        public void setNetworkLocalWifi(double networkLocalWifi) {
            this.networkLocalWifi = networkLocalWifi;
        }

        public double getNetworkVpn() {
            return networkVpn;
        }

        public void setNetworkVpn(double networkVpn) {
            this.networkVpn = networkVpn;
        }

        public double getNetworkRemoteCellular() {
            return networkRemoteCellular;
        }

        public void setNetworkRemoteCellular(double networkRemoteCellular) {
            this.networkRemoteCellular = networkRemoteCellular;
        }

        public double getNetworkUnknown() {
            return networkUnknown;
        }

        public void setNetworkUnknown(double networkUnknown) {
            this.networkUnknown = networkUnknown;
        }

        public double getSensitivityLow() {
            return sensitivityLow;
        }

        public void setSensitivityLow(double sensitivityLow) {
            this.sensitivityLow = sensitivityLow;
        }

        public double getSensitivityMedium() {
            return sensitivityMedium;
        }

        public void setSensitivityMedium(double sensitivityMedium) {
            this.sensitivityMedium = sensitivityMedium;
        }

        public double getSensitivityHigh() {
            return sensitivityHigh;
        }

        public void setSensitivityHigh(double sensitivityHigh) {
            this.sensitivityHigh = sensitivityHigh;
        }

        public double getSensitivityCritical() {
            return sensitivityCritical;
        }

        public void setSensitivityCritical(double sensitivityCritical) {
            this.sensitivityCritical = sensitivityCritical;
        }

        public double getBehaviorNormal() {
            return behaviorNormal;
        }

        public void setBehaviorNormal(double behaviorNormal) {
            this.behaviorNormal = behaviorNormal;
        }

        public double getBehaviorSuspicious() {
            return behaviorSuspicious;
        }

        public void setBehaviorSuspicious(double behaviorSuspicious) {
            this.behaviorSuspicious = behaviorSuspicious;
        }

        public double getBehaviorAbnormal() {
            return behaviorAbnormal;
        }

        public void setBehaviorAbnormal(double behaviorAbnormal) {
            this.behaviorAbnormal = behaviorAbnormal;
        }
    }

    public double getMinimumScore() {
        return minimumScore;
    }

    public void setMinimumScore(double minimumScore) {
        this.minimumScore = minimumScore;
    }

    public double getMaximumScore() {
        return maximumScore;
    }

    public void setMaximumScore(double maximumScore) {
        this.maximumScore = maximumScore;
    }

    public double getLowThreshold() {
        return lowThreshold;
    }

    public void setLowThreshold(double lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    public double getMediumThreshold() {
        return mediumThreshold;
    }

    public void setMediumThreshold(double mediumThreshold) {
        this.mediumThreshold = mediumThreshold;
    }

    public Weights getWeights() {
        return weights;
    }

    public void setWeights(Weights weights) {
        this.weights = weights;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
