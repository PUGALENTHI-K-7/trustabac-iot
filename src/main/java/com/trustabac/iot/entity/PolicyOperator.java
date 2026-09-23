package com.trustabac.iot.entity;

/**
 * Supported comparison operators for evaluation of policy conditions.
 */
public enum PolicyOperator {
    EQUALS,
    NOT_EQUALS,
    IN,
    NOT_IN,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL
}
