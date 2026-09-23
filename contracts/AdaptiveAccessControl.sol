// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title AdaptiveAccessControl
 * @author TrustABAC-IoT Research Prototype
 * @notice Authoritative Smart-Contract Decision Engine for Adaptive Trust- and Risk-Aware Access Control
 * @dev Evaluates compact contextual inputs (ABAC result, Booking validity, Resource Sensitivity, Operation,
 *      Trust Score, Risk Score) to produce final authoritative decisions: DENY (0), RESTRICT (1), ALLOW (2).
 */
contract AdaptiveAccessControl {

    enum Decision {
        DENY,      // 0
        RESTRICT,  // 1
        ALLOW      // 2
    }

    // Owner / Administrator
    address public owner;

    // Configurable Trust & Risk Thresholds (Prototype experimental starting parameters)
    uint8 public trustHigh;    // e.g. 70
    uint8 public trustMedium;  // e.g. 30
    uint8 public riskLow;      // e.g. 30
    uint8 public riskMedium;   // e.g. 70

    // Events
    event OwnershipTransferred(address indexed previousOwner, address indexed newOwner);
    event ThresholdsUpdated(uint8 trustHigh, uint8 trustMedium, uint8 riskLow, uint8 riskMedium);
    event AuthorizationEvaluated(
        bytes32 indexed requestReference,
        bytes32 indexed deviceIdentifierHash,
        Decision decision,
        uint8 trustScore,
        uint8 riskScore,
        uint8 resourceSensitivity,
        uint8 operation,
        uint256 timestamp
    );

    modifier onlyOwner() {
        require(msg.sender == owner, "AdaptiveAccessControl: caller is not the owner");
        _;
    }

    constructor() {
        owner = msg.sender;
        trustHigh = 70;
        trustMedium = 30;
        riskLow = 30;
        riskMedium = 70;
        emit ThresholdsUpdated(70, 30, 30, 70);
    }

    /**
     * @notice Updates the trust evaluation thresholds.
     * @param _trustHigh Threshold score for high trust (>= trustHigh is high trust)
     * @param _trustMedium Threshold score for medium trust (>= trustMedium is medium trust)
     */
    function setTrustThresholds(uint8 _trustHigh, uint8 _trustMedium) external onlyOwner {
        require(_trustHigh > _trustMedium, "AdaptiveAccessControl: trustHigh must be greater than trustMedium");
        trustHigh = _trustHigh;
        trustMedium = _trustMedium;
        emit ThresholdsUpdated(trustHigh, trustMedium, riskLow, riskMedium);
    }

    /**
     * @notice Updates the contextual risk evaluation thresholds.
     * @param _riskLow Threshold score for low risk (<= riskLow is low risk)
     * @param _riskMedium Threshold score for medium risk (<= riskMedium is medium risk)
     */
    function setRiskThresholds(uint8 _riskLow, uint8 _riskMedium) external onlyOwner {
        require(_riskMedium > _riskLow, "AdaptiveAccessControl: riskMedium must be greater than riskLow");
        riskLow = _riskLow;
        riskMedium = _riskMedium;
        emit ThresholdsUpdated(trustHigh, trustMedium, riskLow, riskMedium);
    }

    /**
     * @notice Transfers contract ownership to a new account.
     */
    function transferOwnership(address newOwner) external onlyOwner {
        require(newOwner != address(0), "AdaptiveAccessControl: new owner is zero address");
        emit OwnershipTransferred(owner, newOwner);
        owner = newOwner;
    }

    /**
     * @notice Pure/View function to calculate the adaptive authorization decision.
     * @return decision The authoritative verdict: DENY (0), RESTRICT (1), ALLOW (2)
     * @return reasonCode Numerical reason code explaining the decision
     *         0 = ALLOW_HIGH_TRUST_LOW_RISK
     *         1 = ABAC_FAILED
     *         2 = BOOKING_INACTIVE
     *         3 = TRUST_TOO_LOW
     *         4 = RISK_TOO_HIGH
     *         5 = RESTRICT_INTERMEDIATE
     */
    function calculateDecision(
        bool abacPass,
        bool bookingActive,
        uint8 resourceSensitivity,
        uint8 operation,
        uint8 trustScore,
        uint8 riskScore
    ) public view returns (Decision decision, uint8 reasonCode) {
        // 1. Hard Gate: Structural ABAC eligibility must pass
        if (!abacPass) {
            return (Decision.DENY, 1);
        }

        // 2. Hard Gate: Contextual booking reservation must be active
        if (!bookingActive) {
            return (Decision.DENY, 2);
        }

        // 3. Behavioral Reliability Gate: Trust below medium threshold is immediately denied
        if (trustScore < trustMedium) {
            return (Decision.DENY, 3);
        }

        // 4. Contextual Risk Gate: Risk above medium threshold is immediately denied
        if (riskScore > riskMedium) {
            return (Decision.DENY, 4);
        }

        // 5. Full Access Gate: High trust + Low risk qualifies for ALLOW
        if (trustScore >= trustHigh && riskScore <= riskLow) {
            return (Decision.ALLOW, 0);
        }

        // 6. Adaptive Intermediate Gate: Moderate trust or elevated risk receives RESTRICT
        return (Decision.RESTRICT, 5);
    }

    /**
     * @notice Authoritative state-changing authorization evaluation function.
     *         Executes decision rules and emits the on-chain AuthorizationEvaluated event.
     */
    function evaluateAccess(
        bytes32 requestReference,
        bytes32 deviceIdentifierHash,
        bool abacPass,
        bool bookingActive,
        uint8 resourceSensitivity,
        uint8 operation,
        uint8 trustScore,
        uint8 riskScore
    ) external returns (Decision decision, uint8 reasonCode) {
        (decision, reasonCode) = calculateDecision(
            abacPass,
            bookingActive,
            resourceSensitivity,
            operation,
            trustScore,
            riskScore
        );

        emit AuthorizationEvaluated(
            requestReference,
            deviceIdentifierHash,
            decision,
            trustScore,
            riskScore,
            resourceSensitivity,
            operation,
            block.timestamp
        );

        return (decision, reasonCode);
    }
}
