package com.trustabac.iot.entity;

/**
 * Result outcome of the ABAC eligibility evaluation gate.
 * PASS indicates baseline eligibility (NOT final authorization).
 * FAIL indicates ineligibility with an explanatory reason.
 */
public enum AbacResult {
    PASS,
    FAIL
}
