# TrustABAC-IoT: Final Repository Security Scan Report

- **Scan Date**: `2026-09-23T20:36:30.401792+00:00`
- **Total Files Scanned**: `317`
- **Total Leaked Secrets Found**: `0`
- **Scan Status**: `PASS - CLEAN`

## 1. Scanner Rules & Coverage

| Rule Category | Pattern Checked | Description |
| :--- | :--- | :--- |
| **Private Keys** | `0x[a-fA-F0-9]{64}` | Detects un-obfuscated 64-hex Ethereum/EVM private keys |
| **JWT Tokens** | `ey[A-Za-z0-9_-]{30,}...` | Detects JSON Web Tokens in code or config |
| **Cloud Secrets** | `AKIA...` / `aws_secret...` | Detects cloud provider API tokens |
| **Hardcoded Passwords** | `password = '...'` | Detects cleartext password assignments |
| **Credential URLs** | `http://user:pass@host` | Detects basic authentication strings embedded in URLs |

## 2. Findings Summary

> [!NOTE]
> **Zero Credentials or Private Keys Detected**: All configuration bindings use environment variables and in-memory ephemeral key derivation.
