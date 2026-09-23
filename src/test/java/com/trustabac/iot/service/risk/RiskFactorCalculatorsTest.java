package com.trustabac.iot.service.risk;

import com.trustabac.iot.config.RiskProperties;
import com.trustabac.iot.entity.BehavioralIndicator;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.ResourceSensitivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskFactorCalculatorsTest {

    private RiskProperties riskProperties;
    private TimeRiskCalculator timeRiskCalculator;
    private LocationRiskCalculator locationRiskCalculator;
    private SensitivityRiskCalculator sensitivityRiskCalculator;
    private FrequencyRiskCalculator frequencyRiskCalculator;
    private NetworkRiskCalculator networkRiskCalculator;
    private ViolationRiskCalculator violationRiskCalculator;
    private BehaviorRiskCalculator behaviorRiskCalculator;

    @BeforeEach
    void setUp() {
        riskProperties = new RiskProperties();
        timeRiskCalculator = new TimeRiskCalculator(riskProperties);
        locationRiskCalculator = new LocationRiskCalculator(riskProperties);
        sensitivityRiskCalculator = new SensitivityRiskCalculator(riskProperties);
        frequencyRiskCalculator = new FrequencyRiskCalculator(riskProperties);
        networkRiskCalculator = new NetworkRiskCalculator(riskProperties);
        violationRiskCalculator = new ViolationRiskCalculator(riskProperties);
        behaviorRiskCalculator = new BehaviorRiskCalculator(riskProperties);
    }

    @Test
    @DisplayName("Factor 1: TimeRiskCalculator returns 0.0 during operating hours and 1.0 outside")
    void testTimeRiskCalculator() {
        LocalDateTime daytime = LocalDateTime.of(2026, 9, 20, 14, 0, 0); // 14:00 (inside 6-22)
        RiskContext contextDay = RiskContext.builder().evaluationTimestamp(daytime).build();
        assertEquals(0.0, timeRiskCalculator.calculate(contextDay));

        LocalDateTime nighttime = LocalDateTime.of(2026, 9, 20, 2, 30, 0); // 02:30 (outside 6-22)
        RiskContext contextNight = RiskContext.builder().evaluationTimestamp(nighttime).build();
        assertEquals(1.0, timeRiskCalculator.calculate(contextNight));
    }

    @Test
    @DisplayName("Factor 2: LocationRiskCalculator returns 0.0 for expected location and 1.0 for mismatch")
    void testLocationRiskCalculator() {
        RiskContext contextValid = RiskContext.builder().location("Property-001").build();
        assertEquals(0.0, locationRiskCalculator.calculate(contextValid));

        RiskContext contextUnexpected = RiskContext.builder().location("Remote-Zone-X").build();
        assertEquals(1.0, locationRiskCalculator.calculate(contextUnexpected));

        RiskContext contextNull = RiskContext.builder().location(null).build();
        assertEquals(1.0, locationRiskCalculator.calculate(contextNull));
    }

    @Test
    @DisplayName("Factor 3: SensitivityRiskCalculator returns normalized 0.0, 0.35, 0.70, 1.0")
    void testSensitivityRiskCalculator() {
        assertEquals(0.0, sensitivityRiskCalculator.calculate(RiskContext.builder().resourceSensitivity(ResourceSensitivity.LOW).build()));
        assertEquals(0.35, sensitivityRiskCalculator.calculate(RiskContext.builder().resourceSensitivity(ResourceSensitivity.MEDIUM).build()));
        assertEquals(0.70, sensitivityRiskCalculator.calculate(RiskContext.builder().resourceSensitivity(ResourceSensitivity.HIGH).build()));
        assertEquals(1.0, sensitivityRiskCalculator.calculate(RiskContext.builder().resourceSensitivity(ResourceSensitivity.CRITICAL).build()));
    }

    @Test
    @DisplayName("Factor 4: FrequencyRiskCalculator returns 0.0 at low threshold and 1.0 at high threshold")
    void testFrequencyRiskCalculator() {
        assertEquals(0.0, frequencyRiskCalculator.calculate(RiskContext.builder().requestCountWindow(1).build()));
        assertEquals(0.0, frequencyRiskCalculator.calculate(RiskContext.builder().requestCountWindow(5).build()));
        assertEquals(1.0, frequencyRiskCalculator.calculate(RiskContext.builder().requestCountWindow(20).build()));
        assertEquals(1.0, frequencyRiskCalculator.calculate(RiskContext.builder().requestCountWindow(50).build()));

        double mid = frequencyRiskCalculator.calculate(RiskContext.builder().requestCountWindow(10).build());
        assertTrue(mid > 0.0 && mid < 1.0);
    }

    @Test
    @DisplayName("Factor 5: NetworkRiskCalculator maps connection origins correctly")
    void testNetworkRiskCalculator() {
        assertEquals(0.0, networkRiskCalculator.calculate(RiskContext.builder().networkContext("LOCAL_WIFI").build()));
        assertEquals(0.30, networkRiskCalculator.calculate(RiskContext.builder().networkContext("VPN").build()));
        assertEquals(0.60, networkRiskCalculator.calculate(RiskContext.builder().networkContext("REMOTE_CELLULAR").build()));
        assertEquals(1.0, networkRiskCalculator.calculate(RiskContext.builder().networkContext("PUBLIC_INTERNET").build()));
        assertEquals(1.0, networkRiskCalculator.calculate(RiskContext.builder().networkContext(null).build()));
    }

    @Test
    @DisplayName("Factor 6: ViolationRiskCalculator scales with recent violations")
    void testViolationRiskCalculator() {
        assertEquals(0.0, violationRiskCalculator.calculate(RiskContext.builder().recentViolationCount(0).build()));
        assertEquals(1.0, violationRiskCalculator.calculate(RiskContext.builder().recentViolationCount(5).build()));
        assertEquals(1.0, violationRiskCalculator.calculate(RiskContext.builder().recentViolationCount(10).build()));

        double partial = violationRiskCalculator.calculate(RiskContext.builder().recentViolationCount(2).build());
        assertEquals(0.40, partial, 0.01);
    }

    @Test
    @DisplayName("Factor 7: BehaviorRiskCalculator maps behavioral indicators")
    void testBehaviorRiskCalculator() {
        assertEquals(0.0, behaviorRiskCalculator.calculate(RiskContext.builder().behavioralIndicator(BehavioralIndicator.NORMAL).build()));
        assertEquals(0.60, behaviorRiskCalculator.calculate(RiskContext.builder().behavioralIndicator(BehavioralIndicator.SUSPICIOUS).build()));
        assertEquals(1.0, behaviorRiskCalculator.calculate(RiskContext.builder().behavioralIndicator(BehavioralIndicator.ABNORMAL).build()));
    }
}
