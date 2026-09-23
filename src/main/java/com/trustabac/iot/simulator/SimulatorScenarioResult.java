package com.trustabac.iot.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the complete execution report for a simulator scenario run.
 */
public class SimulatorScenarioResult {

    private String scenarioId;
    private SimulatorScenarioType scenarioType;
    private String scenarioName;
    private String description;
    private String startTime;
    private String endTime;
    private long durationMs;
    private int totalRequests;
    private int allowCount;
    private int restrictCount;
    private int denyCount;
    private int executedCount;
    private int downgradedCount;
    private int blockedCount;
    private List<SimulatorEvent> events = new ArrayList<>();
    private Map<String, Object> finalDeviceStates = new HashMap<>();
    private boolean success = true;
    private String summary;

    public SimulatorScenarioResult() {
    }

    public SimulatorScenarioResult(String scenarioId, SimulatorScenarioType scenarioType, String scenarioName, String description) {
        this.scenarioId = scenarioId;
        this.scenarioType = scenarioType;
        this.scenarioName = scenarioName;
        this.description = description;
    }

    public void addEvent(SimulatorEvent event) {
        if (this.events == null) {
            this.events = new ArrayList<>();
        }
        this.events.add(event);
        this.totalRequests++;

        if (event.decision() != null) {
            switch (event.decision()) {
                case ALLOW -> this.allowCount++;
                case RESTRICT -> this.restrictCount++;
                case DENY -> this.denyCount++;
            }
        }

        if (event.enforcementStatus() != null) {
            switch (event.enforcementStatus()) {
                case EXECUTED -> this.executedCount++;
                case DOWNGRADED -> this.downgradedCount++;
                case BLOCKED -> this.blockedCount++;
            }
        }
    }

    public String getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(String scenarioId) {
        this.scenarioId = scenarioId;
    }

    public SimulatorScenarioType getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(SimulatorScenarioType scenarioType) {
        this.scenarioType = scenarioType;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public int getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(int totalRequests) {
        this.totalRequests = totalRequests;
    }

    public int getAllowCount() {
        return allowCount;
    }

    public void setAllowCount(int allowCount) {
        this.allowCount = allowCount;
    }

    public int getRestrictCount() {
        return restrictCount;
    }

    public void setRestrictCount(int restrictCount) {
        this.restrictCount = restrictCount;
    }

    public int getDenyCount() {
        return denyCount;
    }

    public void setDenyCount(int denyCount) {
        this.denyCount = denyCount;
    }

    public int getExecutedCount() {
        return executedCount;
    }

    public void setExecutedCount(int executedCount) {
        this.executedCount = executedCount;
    }

    public int getDowngradedCount() {
        return downgradedCount;
    }

    public void setDowngradedCount(int downgradedCount) {
        this.downgradedCount = downgradedCount;
    }

    public int getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(int blockedCount) {
        this.blockedCount = blockedCount;
    }

    public List<SimulatorEvent> getEvents() {
        return events;
    }

    public void setEvents(List<SimulatorEvent> events) {
        this.events = events;
    }

    public Map<String, Object> getFinalDeviceStates() {
        return finalDeviceStates;
    }

    public void setFinalDeviceStates(Map<String, Object> finalDeviceStates) {
        this.finalDeviceStates = finalDeviceStates;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
