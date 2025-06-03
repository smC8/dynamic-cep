// RuleMetadata.java
package com.company.rulesengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class RuleMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("ruleId")
    private String ruleId;
    
    @JsonProperty("tenantId")
    private String tenantId;
    
    @JsonProperty("executionCount")
    private long executionCount;
    
    @JsonProperty("matchCount")
    private long matchCount;
    
    @JsonProperty("averageExecutionTime")
    private double averageExecutionTime;
    
    @JsonProperty("lastExecuted")
    private long lastExecuted;
    
    @JsonProperty("errorCount")
    private long errorCount;
    
    @JsonProperty("lastError")
    private String lastError;
    
    @JsonProperty("performance")
    private PerformanceMetrics performance;
    
    @JsonProperty("customMetrics")
    private Map<String, Object> customMetrics;

    // Constructors
    public RuleMetadata() {}
    
    public RuleMetadata(String ruleId, String tenantId) {
        this.ruleId = ruleId;
        this.tenantId = tenantId;
        this.executionCount = 0;
        this.matchCount = 0;
        this.averageExecutionTime = 0.0;
        this.errorCount = 0;
        this.performance = new PerformanceMetrics();
    }

    // Inner class for performance metrics
    public static class PerformanceMetrics implements Serializable {
        private static final long serialVersionUID = 1L;
        
        @JsonProperty("minExecutionTime")
        private long minExecutionTime = Long.MAX_VALUE;
        
        @JsonProperty("maxExecutionTime")
        private long maxExecutionTime = 0;
        
        @JsonProperty("p95ExecutionTime")
        private long p95ExecutionTime = 0;
        
        @JsonProperty("p99ExecutionTime")
        private long p99ExecutionTime = 0;
        
        @JsonProperty("throughput")
        private double throughput = 0.0;

        // Getters and Setters
        public long getMinExecutionTime() { return minExecutionTime; }
        public void setMinExecutionTime(long minExecutionTime) { this.minExecutionTime = minExecutionTime; }
        
        public long getMaxExecutionTime() { return maxExecutionTime; }
        public void setMaxExecutionTime(long maxExecutionTime) { this.maxExecutionTime = maxExecutionTime; }
        
        public long getP95ExecutionTime() { return p95ExecutionTime; }
        public void setP95ExecutionTime(long p95ExecutionTime) { this.p95ExecutionTime = p95ExecutionTime; }
        
        public long getP99ExecutionTime() { return p99ExecutionTime; }
        public void setP99ExecutionTime(long p99ExecutionTime) { this.p99ExecutionTime = p99ExecutionTime; }
        
        public double getThroughput() { return throughput; }
        public void setThroughput(double throughput) { this.throughput = throughput; }
    }

    // Getters and Setters
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public long getExecutionCount() { return executionCount; }
    public void setExecutionCount(long executionCount) { this.executionCount = executionCount; }
    
    public long getMatchCount() { return matchCount; }
    public void setMatchCount(long matchCount) { this.matchCount = matchCount; }
    
    public double getAverageExecutionTime() { return averageExecutionTime; }
    public void setAverageExecutionTime(double averageExecutionTime) { this.averageExecutionTime = averageExecutionTime; }
    
    public long getLastExecuted() { return lastExecuted; }
    public void setLastExecuted(long lastExecuted) { this.lastExecuted = lastExecuted; }
    
    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
    
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    
    public PerformanceMetrics getPerformance() { return performance; }
    public void setPerformance(PerformanceMetrics performance) { this.performance = performance; }
    
    public Map<String, Object> getCustomMetrics() { return customMetrics; }
    public void setCustomMetrics(Map<String, Object> customMetrics) { this.customMetrics = customMetrics; }

    // Utility methods
    public void incrementExecutionCount() {
        this.executionCount++;
    }
    
    public void incrementMatchCount() {
        this.matchCount++;
    }
    
    public void incrementErrorCount() {
        this.errorCount++;
    }
    
    public void updateExecutionTime(long executionTime) {
        this.lastExecuted = System.currentTimeMillis();
        
        // Update average execution time
        this.averageExecutionTime = ((this.averageExecutionTime * (this.executionCount - 1)) + executionTime) / this.executionCount;
        
        // Update performance metrics
        if (this.performance != null) {
            this.performance.minExecutionTime = Math.min(this.performance.minExecutionTime, executionTime);
            this.performance.maxExecutionTime = Math.max(this.performance.maxExecutionTime, executionTime);
        }
    }
    
    public double getMatchRate() {
        return executionCount > 0 ? (double) matchCount / executionCount : 0.0;
    }
    
    public double getErrorRate() {
        return executionCount > 0 ? (double) errorCount / executionCount : 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleMetadata that = (RuleMetadata) o;
        return Objects.equals(ruleId, that.ruleId) &&
               Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleId, tenantId);
    }

    @Override
    public String toString() {
        return "RuleMetadata{" +
               "ruleId='" + ruleId + '\'' +
               ", tenantId='" + tenantId + '\'' +
               ", executionCount=" + executionCount +
               ", matchCount=" + matchCount +
               ", averageExecutionTime=" + averageExecutionTime +
               ", errorCount=" + errorCount +
               '}';
    }
}