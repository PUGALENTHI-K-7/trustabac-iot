package com.trustabac.iot.experiment.dto;

import java.util.Map;

/**
 * DTO providing offline, counterfactual comparison between the production TrustABAC-IoT pipeline (Mode C)
 * and theoretical analytical reference baselines (Mode A: ABAC-Only, Mode B: ABAC+Trust+Risk without blockchain).
 * Note: Modes A and B are pure offline analytical references and never execute device actions or mutate state.
 */
public class ExperimentComparisonResponse {

    private String runId;
    private ScenarioType scenarioType;
    private int sampleSize;
    private Map<String, Object> modeCFullTrustabacBlockchain;
    private Map<String, Object> modeAAbacOnlyAnalyticalReference;
    private Map<String, Object> modeBAbacTrustRiskAnalyticalReference;
    private Map<String, Object> comparativeSummary;

    public ExperimentComparisonResponse() {
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public ScenarioType getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(ScenarioType scenarioType) {
        this.scenarioType = scenarioType;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Map<String, Object> getModeCFullTrustabacBlockchain() {
        return modeCFullTrustabacBlockchain;
    }

    public void setModeCFullTrustabacBlockchain(Map<String, Object> modeCFullTrustabacBlockchain) {
        this.modeCFullTrustabacBlockchain = modeCFullTrustabacBlockchain;
    }

    public Map<String, Object> getModeAAbacOnlyAnalyticalReference() {
        return modeAAbacOnlyAnalyticalReference;
    }

    public void setModeAAbacOnlyAnalyticalReference(Map<String, Object> modeAAbacOnlyAnalyticalReference) {
        this.modeAAbacOnlyAnalyticalReference = modeAAbacOnlyAnalyticalReference;
    }

    public Map<String, Object> getModeBAbacTrustRiskAnalyticalReference() {
        return modeBAbacTrustRiskAnalyticalReference;
    }

    public void setModeBAbacTrustRiskAnalyticalReference(Map<String, Object> modeBAbacTrustRiskAnalyticalReference) {
        this.modeBAbacTrustRiskAnalyticalReference = modeBAbacTrustRiskAnalyticalReference;
    }

    public Map<String, Object> getComparativeSummary() {
        return comparativeSummary;
    }

    public void setComparativeSummary(Map<String, Object> comparativeSummary) {
        this.comparativeSummary = comparativeSummary;
    }
}
