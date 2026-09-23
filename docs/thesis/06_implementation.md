# Chapter 6 — Implementation

## 6.1 Technology Stack and Module Structure
The TrustABAC-IoT platform is implemented using modern enterprise software frameworks and blockchain toolchains:

- **Programming Languages**: Java 21 LTS (OpenJDK 21.0.8), Solidity ^0.8.19, Python 3.13 (Testing & Evaluation Framework), JavaScript ES6.
- **Backend Framework**: Spring Boot 3.2.3 (Spring Security 6.2, Spring Data JPA, Spring AMQP, Spring WebSocket STOMP, Spring Batch 5.1).
- **Blockchain Integration**: Web3j 4.10.3 (Java EVM Client), Truffle Ganache v7.9.2 (Ethereum EVM Testbed).
- **Persistence & Messaging**: MySQL 8.0 Community (Docker), RabbitMQ 3.13 with Management Plugin (Docker).
- **Build & CI Automation**: Apache Maven 3.9, PowerShell, Bash.

---

## 6.2 Smart Contract Implementation (`AdaptiveAccessControl.sol`)
The smart contract defines the immutable decision matrix and state-transition rules on the EVM:

```solidity
// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

contract AdaptiveAccessControl {
    address public owner;
    
    enum Decision { DENY, RESTRICT, ALLOW }
    
    uint8 public trustHigh = 70;
    uint8 public trustMedium = 30;
    uint8 public riskLow = 30;
    uint8 public riskMedium = 70;
    
    event AuthorizationEvaluated(
        bytes32 indexed requestId,
        address indexed subject,
        bytes32 indexed resourceId,
        Decision decision,
        uint8 reasonCode,
        uint8 trustScore,
        uint8 riskScore,
        uint8 resourceSensitivity,
        uint8 operation,
        uint256 timestamp
    );

    constructor() {
        owner = msg.sender;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Caller is not owner");
        _;
    }

    function evaluateAccess(
        bool abacPass,
        bool bookingActive,
        uint8 resourceSensitivity,
        uint8 operation,
        uint8 trustScore,
        uint8 riskScore
    ) external returns (Decision decision, uint8 reasonCode) {
        // Hard-Deny Preconditions
        if (!abacPass) {
            emit AuthorizationEvaluated(..., Decision.DENY, 1, ...);
            return (Decision.DENY, 1);
        }
        if (!bookingActive) {
            emit AuthorizationEvaluated(..., Decision.DENY, 2, ...);
            return (Decision.DENY, 2);
        }
        if (trustScore < trustMedium) {
            emit AuthorizationEvaluated(..., Decision.DENY, 3, ...);
            return (Decision.DENY, 3);
        }
        if (riskScore > riskMedium) {
            emit AuthorizationEvaluated(..., Decision.DENY, 4, ...);
            return (Decision.DENY, 4);
        }

        // Adaptive Evaluation Matrix
        if (trustScore >= trustHigh && riskScore <= riskLow) {
            decision = Decision.ALLOW;
            reasonCode = 0;
        } else {
            decision = Decision.RESTRICT;
            reasonCode = 5;
        }

        emit AuthorizationEvaluated(..., decision, reasonCode, ...);
        return (decision, reasonCode);
    }
}
```

### Deterministic Decision Matrix:
| ABAC Pass? | Booking Active? | Trust Score ($T$) | Risk Score ($R$) | On-Chain Decision | Reason Code | Security Action |
| :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| **False** | Any | Any | Any | `DENY` | 1 | Precondition Attribute Failure |
| **True** | **False** | Any | Any | `DENY` | 2 | Expired/Inactive Booking |
| **True** | **True** | $T < 30$ | Any | `DENY` | 3 | Degraded Subject Trust |
| **True** | **True** | Any | $R > 70$ | `DENY` | 4 | Severe Contextual Risk |
| **True** | **True** | $T \ge 70$ | $R \le 30$ | `ALLOW` | 0 | Optimal Trust & Low Risk |
| **True** | **True** | $30 \le T < 70$ | $R \le 70$ | `RESTRICT` | 5 | Moderate Trust Attenuation |
| **True** | **True** | $T \ge 70$ | $30 < R \le 70$ | `RESTRICT` | 5 | Moderate Risk Attenuation |

---

## 6.3 Core Spring Boot Backend Services

### 1. `AbacService.java`
- Implements Gate 1 attribute evaluation.
- Queries `BookingRepository` to ensure the subject has a valid reservation covering the current timestamp.
- Validates subject role (`GUEST`, `TENANT`, `MAINTENANCE`, `OWNER`) against device access policies.

### 2. `TrustService.java`
- Maintains long-term trust scores in MySQL (`trust_history` table).
- Computes baseline scores via linear decay and recovery equations:
  $$T_{new} = \max(0.0, T_{current} - \Delta_{penalty})$$
  $$T_{new} = \min(100.0, T_{current} + \Delta_{recovery})$$

### 3. `RiskService.java`
- Evaluates stateless contextual risk combining four weighted dimensions:
  $$R = w_s \cdot S_{resource} + w_n \cdot N_{network} + w_t \cdot T_{time} + w_b \cdot B_{burst}$$
- Where weights sum to 1.0 ($w_s=0.40, w_n=0.25, w_t=0.15, w_b=0.20$).

### 4. `DecisionCoordinator.java`
- Orchestrates the multi-tiered pipeline.
- Calls `BlockchainService.evaluateAccessOnChain(...)` via Web3j.
- Implements the **Fail-Closed** exception handler: catches network timeouts, JSON-RPC connection drops, or EVM execution errors and immediately returns `DENY`.

### 5. `ResourceOperationService.java`
- Translates decision enums to physical actions on simulated device models:
  - **Smart Door Lock**: `ALLOW` unlocks door; `RESTRICT` keeps door locked and returns read-only lock status telemetry; `DENY` blocks operation.
  - **Smart Thermostat**: `ALLOW` sets target temperature directly; `RESTRICT` clamps requested temperature to safe eco-bounds ($20.0^\circ\text{C} \le Temp \le 24.0^\circ\text{C}$); `DENY` blocks change.
  - **Smart Light & TV**: `ALLOW` toggles power/channels; `RESTRICT` permits status queries; `DENY` blocks change.

---

## 6.4 Relational Persistence Schema (MySQL)
The relational persistence layer comprises 6 primary JPA entity models:
1. `devices`: Device registration, resource type, sensitivity classification, operational state.
2. `bookings`: Reservation records, property IDs, guest IDs, start/end timestamps, active status flags.
3. `access_requests`: Complete audit trail of every access attempt (request ID, subject ID, device ID, operation, decision, enforcement status, latency timestamps, blockchain transaction hash, block number).
4. `trust_history`: Granular log of trust updates, penalty events, and recovery triggers.
5. `risk_events`: Recorded contextual anomalies, burst frequency alerts, and network mismatch logs.
6. `batch_run_audit`: Idempotent Spring Batch analytical summaries, period keys, and source reconciliation flags.
