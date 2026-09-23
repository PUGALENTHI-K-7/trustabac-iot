package com.trustabac.iot.entity;

/**
 * Immediate contextual behavioral indicators observed for an access request.
 * Note: Represents immediate situational behavior for Risk calculation,
 * strictly distinct from long-term device Trust history.
 */
public enum BehavioralIndicator {
    NORMAL,
    SUSPICIOUS,
    ABNORMAL
}
