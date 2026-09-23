package com.trustabac.iot.service;

import com.trustabac.iot.entity.AttributeCategory;
import com.trustabac.iot.entity.Policy;
import com.trustabac.iot.entity.PolicyCondition;
import com.trustabac.iot.entity.PolicyOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyEvaluatorTest {

    private PolicyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new PolicyEvaluator();
    }

    @Test
    @DisplayName("EQUALS operator: matches string, boolean, and numeric values case-insensitively")
    void testEqualsOperator() {
        PolicyCondition condStr = new PolicyCondition(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.EQUALS, "GUEST");
        PolicyCondition condBool = new PolicyCondition(AttributeCategory.DEVICE, "device.active", PolicyOperator.EQUALS, "true");
        PolicyCondition condNum = new PolicyCondition(AttributeCategory.DEVICE, "device.trust", PolicyOperator.EQUALS, "80.0");

        Map<String, Object> attrs = Map.of(
                "subject.role", "guest",
                "device.active", "TRUE",
                "device.trust", "80.00"
        );

        assertTrue(evaluator.evaluateCondition(condStr, attrs).passed());
        assertTrue(evaluator.evaluateCondition(condBool, attrs).passed());
        assertTrue(evaluator.evaluateCondition(condNum, attrs).passed());
    }

    @Test
    @DisplayName("NOT_EQUALS operator: passes when values differ and fails when values match")
    void testNotEqualsOperator() {
        PolicyCondition cond = new PolicyCondition(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.NOT_EQUALS, "ADMIN");

        assertTrue(evaluator.evaluateCondition(cond, Map.of("subject.role", "GUEST")).passed());
        assertFalse(evaluator.evaluateCondition(cond, Map.of("subject.role", "admin")).passed());
    }

    @Test
    @DisplayName("IN operator: matches when actual value is in the comma-separated expected list")
    void testInOperator() {
        PolicyCondition cond = new PolicyCondition(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.IN, "GUEST, OWNER, MAINTENANCE");

        assertTrue(evaluator.evaluateCondition(cond, Map.of("subject.role", "GUEST")).passed());
        assertTrue(evaluator.evaluateCondition(cond, Map.of("subject.role", "owner")).passed());
        assertFalse(evaluator.evaluateCondition(cond, Map.of("subject.role", "ANONYMOUS")).passed());
    }

    @Test
    @DisplayName("NOT_IN operator: passes when actual value is absent from expected list")
    void testNotInOperator() {
        PolicyCondition cond = new PolicyCondition(AttributeCategory.RESOURCE, "resource.sensitivity", PolicyOperator.NOT_IN, "HIGH, CRITICAL");

        assertTrue(evaluator.evaluateCondition(cond, Map.of("resource.sensitivity", "LOW")).passed());
        assertTrue(evaluator.evaluateCondition(cond, Map.of("resource.sensitivity", "MEDIUM")).passed());
        assertFalse(evaluator.evaluateCondition(cond, Map.of("resource.sensitivity", "HIGH")).passed());
    }

    @Test
    @DisplayName("Numeric comparison operators: GREATER_THAN, LESS_THAN, GTE, LTE")
    void testNumericOperators() {
        PolicyCondition gt = new PolicyCondition(AttributeCategory.CONTEXT, "trust.score", PolicyOperator.GREATER_THAN, "70.0");
        PolicyCondition lt = new PolicyCondition(AttributeCategory.CONTEXT, "risk.score", PolicyOperator.LESS_THAN, "50.0");
        PolicyCondition gte = new PolicyCondition(AttributeCategory.CONTEXT, "trust.score", PolicyOperator.GREATER_THAN_OR_EQUAL, "70.0");
        PolicyCondition lte = new PolicyCondition(AttributeCategory.CONTEXT, "risk.score", PolicyOperator.LESS_THAN_OR_EQUAL, "50.0");

        Map<String, Object> validAttrs = Map.of(
                "trust.score", "75.5",
                "risk.score", "30.0"
        );

        assertTrue(evaluator.evaluateCondition(gt, validAttrs).passed());
        assertTrue(evaluator.evaluateCondition(lt, validAttrs).passed());
        assertTrue(evaluator.evaluateCondition(gte, validAttrs).passed());
        assertTrue(evaluator.evaluateCondition(lte, validAttrs).passed());

        // Boundary equality checks
        Map<String, Object> boundaryAttrs = Map.of(
                "trust.score", "70.0",
                "risk.score", "50.0"
        );

        assertFalse(evaluator.evaluateCondition(gt, boundaryAttrs).passed());
        assertTrue(evaluator.evaluateCondition(gte, boundaryAttrs).passed());
        assertFalse(evaluator.evaluateCondition(lt, boundaryAttrs).passed());
        assertTrue(evaluator.evaluateCondition(lte, boundaryAttrs).passed());
    }

    @Test
    @DisplayName("Evaluation fails safely when numeric values are malformed")
    void testMalformedNumericComparison() {
        PolicyCondition cond = new PolicyCondition(AttributeCategory.CONTEXT, "trust.score", PolicyOperator.GREATER_THAN, "70.0");

        Map<String, Object> malformedAttrs = Map.of("trust.score", "not-a-number");
        assertFalse(evaluator.evaluateCondition(cond, malformedAttrs).passed());
    }

    @Test
    @DisplayName("Evaluation fails explainably when required attribute is missing from context")
    void testMissingAttribute() {
        PolicyCondition cond = new PolicyCondition(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.EQUALS, "GUEST");

        Map<String, Object> emptyAttrs = new HashMap<>();
        PolicyEvaluator.ConditionEvaluationResult result = evaluator.evaluateCondition(cond, emptyAttrs);

        assertFalse(result.passed());
        assertNotNull(result.reason());
        assertTrue(result.reason().contains("Missing required attribute 'subject.role'"));
    }

    @Test
    @DisplayName("evaluatePolicy: evaluates multiple conditions and succeeds only when ALL conditions pass")
    void testEvaluatePolicyMultiConditions() {
        Policy policy = new Policy("Door Policy", "Description", "SMART_DOOR_LOCK", null, true);
        policy.addCondition(new PolicyCondition(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.EQUALS, "GUEST"));
        policy.addCondition(new PolicyCondition(AttributeCategory.DEVICE, "device.active", PolicyOperator.EQUALS, "true"));
        policy.addCondition(new PolicyCondition(AttributeCategory.CONTEXT, "booking.valid", PolicyOperator.EQUALS, "true"));

        Map<String, Object> validAttrs = Map.of(
                "subject.role", "GUEST",
                "device.active", "true",
                "booking.valid", "true"
        );

        assertTrue(evaluator.evaluatePolicy(policy, validAttrs).passed());

        Map<String, Object> invalidBookingAttrs = Map.of(
                "subject.role", "GUEST",
                "device.active", "true",
                "booking.valid", "false"
        );

        PolicyEvaluator.ConditionEvaluationResult failedResult = evaluator.evaluatePolicy(policy, invalidBookingAttrs);
        assertFalse(failedResult.passed());
        assertTrue(failedResult.reason().contains("booking.valid"));
    }
}
