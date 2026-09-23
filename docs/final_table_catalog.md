# TrustABAC-IoT: Final Table Catalog

This catalog documents the 8 empirical research tables synthesized in Chapter 8 and Chapter 7 of the thesis, sourced directly from the validated Phase 8C analysis engine ([`contracts/phase8c_analysis/thesis_tables.md`](file:///j:/PROJECT/TRUST%20-ABAC/trustabac-iot/contracts/phase8c_analysis/thesis_tables.md)).

---

| Table ID | Table Title | Source Artifact | Thesis Chapter | Core Interpretation / Key Findings |
| :---: | :--- | :--- | :---: | :--- |
| **Table 1** | Experimental Environment and Testbed Specification | System configuration & hardware logs | Chapter 7 | Details the exact host specifications (Windows 11 x86_64), Java 21 LTS runtime, MySQL 8.0 container, RabbitMQ 3.13 broker, and Truffle Ganache EVM testbed parameters. |
| **Table 2** | Controlled Security Scenario Definitions and Evaluation Workloads | Benchmark workload definitions | Chapter 7 | Defines the 8 evaluation scenarios, booking status parameters, baseline trust scores, contextual risk drivers, and expected policy outcomes. |
| **Table 3** | Per-Scenario Latency Statistics and Confidence Intervals | `descriptive_statistics.csv` | Chapter 8 | Reports sample mean $\pm$ SE, median, $p95$, and Student's t $CI_{95}$ across authorization and enforcement tiers ($N = 90$ per scenario). Confirms $61.2$ ms normal access vs. $34.8$ ms pre-EVM ABAC failure. |
| **Table 4** | Sequential Request Processing Throughput | `descriptive_statistics.csv` | Chapter 8 | Summarizes sequential client throughput ($13.98$--$24.54$ ops/sec) and mean client E2E batch durations ($1.88$--$3.02$ seconds per 30-op batch). |
| **Table 5** | Policy Decision and Resource Enforcement Distributions | `descriptive_statistics.csv` | Chapter 8 | Validates 100.0% match rate across all 720 requests ($221$ ALLOW / EXECUTED, $110$ RESTRICT / DOWNGRADED, $389$ DENY / BLOCKED). |
| **Table 6** | Observed Trust and Contextual Risk Distributions | `experiment_results_phase8b.json` | Chapter 8 | Catalogs observed trust ranges ($20.0$--$80.0$) and contextual risk averages across scenarios, illustrating how environmental factors drive policy attenuation. |
| **Table 7** | Reference Mode Agreement and Counterfactual Divergence Analysis | `mode_comparison.csv` | Chapter 8 & 10 | Documents Mode A (Pure ABAC) divergence on 56.81% of requests versus Mode B (Centralized T-ABAC) 100.0% policy decision concordance against Mode C. |
| **Table 8** | Blockchain Transaction and Gas Consumption Metrics | `blockchain_analysis.csv` | Chapter 8 | Confirms deterministic $31,863$ gas per on-chain transaction ($421$ transactions, $13,414,323$ total gas) and highlights zero gas expenditure for $299$ pre-EVM filtered requests. |
