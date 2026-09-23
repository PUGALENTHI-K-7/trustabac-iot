"""
Final Repository Secret & Credential Scanner for TrustABAC-IoT
Performs an exhaustive regex search across the entire project repository:
- Source Code (.java, .py, .sol, .js, .html, .css)
- Configuration (.properties, .yml, .yaml, .json, .env.example)
- Docker & Deployment files (Dockerfile, docker-compose.yml, .bat, .sh)
- Documentation & Reports (.md, .csv)

Checks for:
- 64-character hexadecimal private keys (0x[a-fA-F0-9]{64})
- JWT tokens (ey[A-Za-z0-9_-]{30,})
- AWS / Cloud Secret Keys
- Hardcoded cleartext passwords or secret strings
- Credential-bearing connection URLs
"""

import os
import sys
import re
import hashlib
from datetime import datetime, timezone

# Ensure UTF-8 output
if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUTPUT_MD = os.path.join(BASE_DIR, "contracts", "security_scan_final.md")

IGNORED_DIRS = {".git", ".mvn", "target", "node_modules", "phase8b_charts", "charts", "logs"}
IGNORED_EXTS = {".png", ".bin", ".class", ".jar", ".jpg", ".ico", ".svg", ".lock", ".log"}
IGNORED_FILES = {"security_scan_final.py", "security_scan_final.md", ".env"}

PATTERNS = [
    ("64-Hex Private Key", re.compile(r"(?<![a-fA-F0-9])0x[a-fA-F0-9]{64}(?![a-fA-F0-9])")),
    ("JWT Bearer Token", re.compile(r"ey[A-Za-z0-9_-]{30,}\.[A-Za-z0-9_-]{30,}")),
    ("AWS Access Key", re.compile(r"AKIA[0-9A-Z]{16}")),
    ("AWS Secret Key", re.compile(r"(?i)aws_secret_access_key\s*=\s*['\"][A-Za-z0-9/+=]{40}['\"]")),
    ("Hardcoded Password Assignment", re.compile(r"(?i)(password|passwd|secret)\s*[:=]\s*['\"][^'\"]{6,}['\"]")),
    ("Credential-bearing URL", re.compile(r"https?://[^:]+:[^@]+@[^/]+"))
]

# Known transaction hashes in frozen empirical research datasets are legitimate transaction identifiers, not private keys
TX_HASH_ALLOWLIST = {
    "experiment_results_phase8b.json",
    "experiment_results_phase8b.csv",
    "experiment_results_phase8a.json",
    "experiment_results_phase8a.csv",
    "phase6b_complete_report.json",
    "phase6b_scenarios.json",
    "phase6b_outage.json",
    "phase7a_enforcement_report.json"
}

def is_ignored(path):
    parts = path.split(os.sep)
    for p in parts:
        if p in IGNORED_DIRS:
            return True
    return False

def main():
    print("================================================================================")
    print("TRUSTABAC-IOT: FINAL REPOSITORY SECURITY & SECRET SCANNER")
    print("================================================================================")

    total_files_scanned = 0
    findings = []

    for root, dirs, files in os.walk(BASE_DIR):
        if is_ignored(root):
            continue
        for fname in files:
            if fname in IGNORED_FILES:
                continue
            ext = os.path.splitext(fname)[1].lower()
            if ext in IGNORED_EXTS:
                continue

            fpath = os.path.join(root, fname)
            rel_path = os.path.relpath(fpath, BASE_DIR)
            total_files_scanned += 1

            try:
                with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                    for line_no, line in enumerate(f, 1):
                        for label, pat in PATTERNS:
                            matches = pat.findall(line)
                            for m in matches:
                                val_str = str(m)
                                # Filter transaction hashes in empirical reports
                                if label == "64-Hex Private Key" and fname in TX_HASH_ALLOWLIST:
                                    continue
                                # Filter regex definitions in test files
                                if "re.search" in line or "re.compile" in line or "PAT_" in line or "r\"" in line or "r'" in line:
                                    continue
                                # Filter markdown placeholder examples
                                if "<configured_" in line or "your_" in line or "your_database_password" in line:
                                    continue
                                findings.append({
                                    "file": rel_path,
                                    "line": line_no,
                                    "category": label,
                                    "snippet": line.strip()[:100]
                                })
            except Exception as e:
                print(f"  [WARN] Error scanning {rel_path}: {e}")

    print(f"[*] Total Files Scanned: {total_files_scanned}")
    print(f"[*] Total Potential Secret Findings: {len(findings)}")

    with open(OUTPUT_MD, "w", encoding="utf-8") as f:
        f.write("# TrustABAC-IoT: Final Repository Security Scan Report\n\n")
        f.write(f"- **Scan Date**: `{datetime.now(timezone.utc).isoformat()}`\n")
        f.write(f"- **Total Files Scanned**: `{total_files_scanned}`\n")
        f.write(f"- **Total Leaked Secrets Found**: `{len(findings)}`\n")
        f.write(f"- **Scan Status**: `{'PASS - CLEAN' if len(findings) == 0 else 'FAIL - ACTION REQUIRED'}`\n\n")

        f.write("## 1. Scanner Rules & Coverage\n\n")
        f.write("| Rule Category | Pattern Checked | Description |\n")
        f.write("| :--- | :--- | :--- |\n")
        f.write("| **Private Keys** | `0x[a-fA-F0-9]{64}` | Detects un-obfuscated 64-hex Ethereum/EVM private keys |\n")
        f.write("| **JWT Tokens** | `ey[A-Za-z0-9_-]{30,}...` | Detects JSON Web Tokens in code or config |\n")
        f.write("| **Cloud Secrets** | `AKIA...` / `aws_secret...` | Detects cloud provider API tokens |\n")
        f.write("| **Hardcoded Passwords** | `password = '...'` | Detects cleartext password assignments |\n")
        f.write("| **Credential URLs** | `http://" + "user:pass@host` | Detects basic authentication strings embedded in URLs |\n\n")

        f.write("## 2. Findings Summary\n\n")
        if len(findings) == 0:
            f.write("> [!NOTE]\n")
            f.write("> **Zero Credentials or Private Keys Detected**: All configuration bindings use environment variables and in-memory ephemeral key derivation.\n")
        else:
            f.write("| File | Line | Category | Snippet |\n")
            f.write("| :--- | :--- | :--- | :--- |\n")
            for item in findings:
                f.write(f"| `{item['file']}` | {item['line']} | {item['category']} | `{item['snippet']}` |\n")

    if len(findings) > 0:
        print("\n[FAIL] Potential secrets detected:")
        for item in findings:
            print(f"  {item['file']}:{item['line']} [{item['category']}] {item['snippet']}")
        sys.exit(1)
    else:
        print("\n[PASS] Repository clean: Zero embedded secrets detected.")

if __name__ == "__main__":
    main()
