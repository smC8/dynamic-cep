// Tenant.java
package com.company.rulesengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class Tenant implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("tenantId")
    private String tenantId;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty("active")
    private boolean active;
    
    @JsonProperty("configuration")
    private Map<String, Object> configuration;
    
    @JsonProperty("resourceLimits")
    private ResourceLimits resourceLimits;
    
    @JsonProperty("createdAt")
    private long createdAt;
    
    @JsonProperty("updatedAt")
    private long updatedAt;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    // Constructors
    public Tenant() {}
    
    public Tenant(String tenantId, String name) {
        this.tenantId = tenantId;
        this.name = name;
        this.active = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Inner class for resource limits
    public static class ResourceLimits implements Serializable {
        private static final long serialVersionUID = 1L;
        
        @JsonProperty("maxRules")
        private int maxRules;
        
        @JsonProperty("maxEventsPerSecond")
        private int maxEventsPerSecond;
        
        @JsonProperty("maxMemoryMB")
        private int maxMemoryMB;
        
        @JsonProperty("maxCpuCores")
        private double maxCpuCores;

        // Constructors
        public ResourceLimits() {}
        
        public ResourceLimits(int maxRules, int maxEventsPerSecond, int maxMemoryMB, double maxCpuCores) {
            this.maxRules = maxRules;
            this.maxEventsPerSecond = maxEventsPerSecond;
            this.maxMemoryMB = maxMemoryMB;
            this.maxCpuCores = maxCpuCores;
        }

        // Getters and Setters
        public int getMaxRules() { return maxRules; }
        public void setMaxRules(int maxRules) { this.maxRules = maxRules; }
        
        public int getMaxEventsPerSecond() { return maxEventsPerSecond; }
        public void setMaxEventsPerSecond(int maxEventsPerSecond) { this.maxEventsPerSecond = maxEventsPerSecond; }
        
        public int getMaxMemoryMB() { return maxMemoryMB; }
        public void setMaxMemoryMB(int maxMemoryMB) { this.maxMemoryMB = maxMemoryMB; }
        
        public double getMaxCpuCores() { return maxCpuCores; }
        public void setMaxCpuCores(double maxCpuCores) { this.maxCpuCores = maxCpuCores; }
    }

    // Getters and Setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public Map<String, Object> getConfiguration() { return configuration; }
    public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
    
    public ResourceLimits getResourceLimits() { return resourceLimits; }
    public void setResourceLimits(ResourceLimits resourceLimits) { this.resourceLimits = resourceLimits; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(tenantId, tenant.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId);
    }

    @Override
    public String toString() {
        return "Tenant{" +
               "tenantId='" + tenantId + '\'' +
               ", name='" + name + '\'' +
               ", active=" + active +
               '}';
    }
}