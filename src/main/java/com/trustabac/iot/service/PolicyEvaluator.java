package com.trustabac.iot.service;

import com.trustabac.iot.entity.Policy;
import com.trustabac.iot.entity.PolicyCondition;
import com.trustabac.iot.entity.PolicyOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic, explainable evaluator for ABAC policy conditions and predicates.
 * Safe against malformed inputs and does not use dynamic code execution or reflection.
 */
@Component
public class PolicyEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PolicyEvaluator.class);

    /**
     * Evaluation result holder containing the boolean outcome and detailed explanation.
     */
    public record ConditionEvaluationResult(boolean passed, String reason) {
        public static ConditionEvaluationResult pass() {
            return new ConditionEvaluationResult(true, null);
        }

        public static ConditionEvaluationResult fail(String reason) {
            return new ConditionEvaluationResult(false, reason);
        }
    }

    /**
     * Evaluates a full Policy by verifying that all of its conditions are satisfied.
     *
     * @param policy     the Policy entity to evaluate
     * @param attributes the canonical resolved attribute dictionary
     * @return ConditionEvaluationResult indicating PASS if all conditions match, or FAIL with specific reason
     */
    public ConditionEvaluationResult evaluatePolicy(Policy policy, Map<String, Object> attributes) {
        if (policy == null || !Boolean.TRUE.equals(policy.getActive())) {
            return ConditionEvaluationResult.fail("Policy is null or inactive");
        }

        if (policy.getConditions() == null || policy.getConditions().isEmpty()) {
            return ConditionEvaluationResult.pass();
        }

        for (PolicyCondition condition : policy.getConditions()) {
            ConditionEvaluationResult result = evaluateCondition(condition, attributes);
            if (!result.passed()) {
                log.debug("Policy '{}' failed condition on key '{}': {}",
                        policy.getName(), condition.getAttributeKey(), result.reason());
                return result;
            }
        }

        return ConditionEvaluationResult.pass();
    }

    /**
     * Evaluates a single PolicyCondition against the resolved attribute dictionary.
     */
    public ConditionEvaluationResult evaluateCondition(PolicyCondition condition, Map<String, Object> attributes) {
        if (condition == null) {
            return ConditionEvaluationResult.fail("Condition is null");
        }

        String key = condition.getAttributeKey();
        PolicyOperator operator = condition.getOperator();
        String expectedValue = condition.getExpectedValue();

        if (key == null || operator == null || expectedValue == null) {
            return ConditionEvaluationResult.fail("Malformed policy condition: missing key, operator, or expected value");
        }

        Object actualValueObj = attributes.get(key);
        String actualValue = actualValueObj != null ? String.valueOf(actualValueObj).trim() : null;

        if (actualValue == null) {
            return ConditionEvaluationResult.fail("Missing required attribute '" + key + "' in evaluation context");
        }

        boolean match = switch (operator) {
            case EQUALS -> compareEquals(actualValue, expectedValue.trim());
            case NOT_EQUALS -> !compareEquals(actualValue, expectedValue.trim());
            case IN -> evaluateIn(actualValue, expectedValue.trim());
            case NOT_IN -> !evaluateIn(actualValue, expectedValue.trim());
            case GREATER_THAN -> compareNumeric(actualValue, expectedValue.trim(), Comparison.GT);
            case LESS_THAN -> compareNumeric(actualValue, expectedValue.trim(), Comparison.LT);
            case GREATER_THAN_OR_EQUAL -> compareNumeric(actualValue, expectedValue.trim(), Comparison.GTE);
            case LESS_THAN_OR_EQUAL -> compareNumeric(actualValue, expectedValue.trim(), Comparison.LTE);
        };

        if (match) {
            return ConditionEvaluationResult.pass();
        } else {
            return ConditionEvaluationResult.fail(
                    String.format("Condition failed on '%s': expected '%s' %s, actual was '%s'",
                            key, expectedValue, operator, actualValue));
        }
    }

    private boolean compareEquals(String actual, String expected) {
        if (isBoolean(actual) && isBoolean(expected)) {
            return Boolean.parseBoolean(actual) == Boolean.parseBoolean(expected);
        }
        if (isNumeric(actual) && isNumeric(expected)) {
            try {
                return Double.compare(Double.parseDouble(actual), Double.parseDouble(expected)) == 0;
            } catch (NumberFormatException ignored) {
            }
        }
        return actual.equalsIgnoreCase(expected);
    }

    private boolean evaluateIn(String actual, String expectedListStr) {
        String[] tokens = expectedListStr.split(",");
        return Arrays.stream(tokens)
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .anyMatch(token -> compareEquals(actual, token));
    }

    private enum Comparison {
        GT, LT, GTE, LTE
    }

    private boolean compareNumeric(String actual, String expected, Comparison comparison) {
        try {
            double actualNum = Double.parseDouble(actual);
            double expectedNum = Double.parseDouble(expected);
            return switch (comparison) {
                case GT -> actualNum > expectedNum;
                case LT -> actualNum < expectedNum;
                case GTE -> actualNum >= expectedNum;
                case LTE -> actualNum <= expectedNum;
            };
        } catch (NumberFormatException ex) {
            log.warn("Numeric comparison failed due to invalid number format: actual='{}', expected='{}'", actual, expected);
            return false;
        }
    }

    private boolean isBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
