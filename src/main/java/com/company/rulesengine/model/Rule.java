// Rule.java
package com.company.rulesengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class Rule implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("tenantId")
    private String tenantId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("type")
    private String type; // FRAUD_DETECTION, RECOMMENDATION, etc.
    
    @JsonProperty("expression")
    private String expression;
    
    @JsonProperty("priority")
    private int priority;
    
    @JsonProperty("active")
    private boolean active;
    
    @JsonProperty("version")
    private int version;
    
    @JsonProperty("createdAt")
    private long createdAt;
    
    @JsonProperty("updatedAt")
    private long updatedAt;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    @JsonProperty("actions")
    private Map<String, Object> actions;

    // Constructors
    public Rule() {}
    
    public Rule(String id, String tenantId, String name, String type, String expression) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.type = type;
        this.expression = expression;
        this.active = true;
        this.version = 1;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Map<String, Object> getActions() { return actions; }
    public void setActions(Map<String, Object> actions) { this.actions = actions; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule = (Rule) o;
        return Objects.equals(id, rule.id) &&
               Objects.equals(tenantId, rule.tenantId) &&
               Objects.equals(version, rule.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tenantId, version);
    }

    @Override
    public String toString() {
        return "Rule{" +
               "id='" + id + '\'' +
               ", tenantId='" + tenantId + '\'' +
               ", name='" + name + '\'' +
               ", type='" + type + '\'' +
               ", active=" + active +
               ", version=" + version +
               '}';
    }
}