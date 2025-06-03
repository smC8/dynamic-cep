// RuleResult.java
package com.company.rulesengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class RuleResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("resultId")
    private String resultId;
    
    @JsonProperty("tenantId")
    private String tenantId;
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("ruleId")
    private String ruleId;
    
    @JsonProperty("ruleType")
    private String ruleType;
    
    @JsonProperty("matched")
    private boolean matched;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("processingTime")
    private long processingTime;
    
    @JsonProperty("score")
    private double score;
    
    @JsonProperty("actions")
    private Map<String, Object> actions;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("error")
    private String error;
    
    // Window aggregation fields
    @JsonProperty("windowStart")
    private Long windowStart;
    
    @JsonProperty("windowEnd")
    private Long windowEnd;
    
    @JsonProperty("matchCount")
    private Integer matchCount;
    
    @JsonProperty("averageProcessingTime")
    private Long averageProcessingTime;

    // Constructors
    public RuleResult() {
        this.resultId = java.util.UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private RuleResult result = new RuleResult();
        
        public Builder event(Event event) {
            result.eventId = event.getEventId();
            result.tenantId = event.getTenantId();
            return this;
        }
        
        public Builder rule(Rule rule) {
            result.ruleId = rule.getId();
            result.ruleType = rule.getType();
            result.actions = rule.getActions();
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            result.tenantId = tenantId;
            return this;
        }
        
        public Builder ruleType(String ruleType) {
            result.ruleType = ruleType;
            return this;
        }
        
        public Builder matched(boolean matched) {
            result.matched = matched;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            result.timestamp = timestamp;
            return this;
        }
        
        public Builder processingTime(long processingTime) {
            result.processingTime = processingTime;
            return this;
        }
        
        public Builder score(double score) {
            result.score = score;
            return this;
        }
        
        public Builder windowStart(long windowStart) {
            result.windowStart = windowStart;
            return this;
        }
        
        public Builder windowEnd(long windowEnd) {
            result.windowEnd = windowEnd;
            return this;
        }
        
        public Builder matchCount(int matchCount) {
            result.matchCount = matchCount;
            return this;
        }
        
        public Builder averageProcessingTime(long avgTime) {
            result.averageProcessingTime = avgTime;
            return this;
        }
        
        public Builder error(String error) {
            result.error = error;
            return this;
        }
        
        public RuleResult build() {
            return result;
        }
    }
    
    // Static factory methods
    public static RuleResult createError(Event event, String error) {
        return builder()
            .event(event)
            .error(error)
            .matched(false)
            .build();
    }
    
    public static RuleResult createError(Event event, Rule rule, String error) {
        return builder()
            .event(event)
            .rule(rule)
            .error(error)
            .matched(false)
            .build();
    }

    // Getters and Setters
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    
    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }
    
    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public long getProcessingTime() { return processingTime; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
    
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    
    public Map<String, Object> getActions() { return actions; }
    public void setActions(Map<String, Object> actions) { this.actions = actions; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public Long getWindowStart() { return windowStart; }
    public void setWindowStart(Long windowStart) { this.windowStart = windowStart; }
    
    public Long getWindowEnd() { return windowEnd; }
    public void setWindowEnd(Long windowEnd) { this.windowEnd = windowEnd; }
    
    public Integer getMatchCount() { return matchCount; }
    public void setMatchCount(Integer matchCount) { this.matchCount = matchCount; }
    
    public Long getAverageProcessingTime() { return averageProcessingTime; }
    public void setAverageProcessingTime(Long averageProcessingTime) { this.averageProcessingTime = averageProcessingTime; }

    // Get rule reference for aggregation
    public Rule getRule() {
        Rule rule = new Rule();
        rule.setId(this.ruleId);
        rule.setType(this.ruleType);
        return rule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleResult that = (RuleResult) o;
        return Objects.equals(resultId, that.resultId) &&
               Objects.equals(tenantId, that.tenantId) &&
               Objects.equals(eventId, that.eventId) &&
               Objects.equals(ruleId, that.ruleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resultId, tenantId, eventId, ruleId);
    }

    @Override
    public String toString() {
        return "RuleResult{" +
               "resultId='" + resultId + '\'' +
               ", tenantId='" + tenantId + '\'' +
               ", eventId='" + eventId + '\'' +
               ", ruleId='" + ruleId + '\'' +
               ", ruleType='" + ruleType + '\'' +
               ", matched=" + matched +
               ", timestamp=" + timestamp +
               ", processingTime=" + processingTime +
               '}';
    }
}