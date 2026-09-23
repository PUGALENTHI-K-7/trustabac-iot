package com.trustabac.iot.config;

import com.trustabac.iot.entity.TrustEventType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Type-safe configuration properties for the Dynamic Trust Engine.
 * Configurable prototype parameters for empirical research and experimentation.
 */
@Configuration
@ConfigurationProperties(prefix = "trustabac.trust")
public class TrustProperties {

    private double initialScore = 80.0;
    private double minimumScore = 0.0;
    private double maximumScore = 100.0;

    private Delta delta = new Delta();

    public static class Delta {
        private double normalSuccess = 1.0;
        private double suspiciousActivity = -10.0;
        private double requestFlooding = -25.0;
        private double confirmedMalicious = -40.0;
        private double recovery = 10.0;

        public double getNormalSuccess() {
            return normalSuccess;
        }

        public void setNormalSuccess(double normalSuccess) {
            this.normalSuccess = normalSuccess;
        }

        public double getSuspiciousActivity() {
            return suspiciousActivity;
        }

        public void setSuspiciousActivity(double suspiciousActivity) {
            this.suspiciousActivity = suspiciousActivity;
        }

        public double getRequestFlooding() {
            return requestFlooding;
        }

        public void setRequestFlooding(double requestFlooding) {
            this.requestFlooding = requestFlooding;
        }

        public double getConfirmedMalicious() {
            return confirmedMalicious;
        }

        public void setConfirmedMalicious(double confirmedMalicious) {
            this.confirmedMalicious = confirmedMalicious;
        }

        public double getRecovery() {
            return recovery;
        }

        public void setRecovery(double recovery) {
            this.recovery = recovery;
        }
    }

    public double getDeltaForEvent(TrustEventType eventType) {
        if (eventType == null) {
            return 0.0;
        }
        return switch (eventType) {
            case NORMAL_SUCCESS -> delta.getNormalSuccess();
            case SUSPICIOUS_ACTIVITY -> delta.getSuspiciousActivity();
            case REQUEST_FLOODING -> delta.getRequestFlooding();
            case CONFIRMED_MALICIOUS -> delta.getConfirmedMalicious();
            case RECOVERY -> delta.getRecovery();
        };
    }

    public double getInitialScore() {
        return initialScore;
    }

    public void setInitialScore(double initialScore) {
        this.initialScore = initialScore;
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

    public Delta getDelta() {
        return delta;
    }

    public void setDelta(Delta delta) {
        this.delta = delta;
    }
}
