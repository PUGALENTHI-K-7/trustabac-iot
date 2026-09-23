package com.trustabac.iot.entity;

/**
 * Observable enforcement outcome of a protected IoT resource operation.
 */
public enum EnforcementStatus {
    /**
     * Requested operation was authorized and fully executed on the resource.
     */
    EXECUTED,

    /**
     * Requested operation was downgraded or throttled according to sensitivity-aware adaptive policy
     * (e.g. physical control suppressed on sensitive device, telemetry read permitted, or safe parameter bounds enforced).
     */
    DOWNGRADED,

    /**
     * Operation was blocked due to authorization denial, policy failure, or fail-closed enforcement.
     */
    BLOCKED
}
