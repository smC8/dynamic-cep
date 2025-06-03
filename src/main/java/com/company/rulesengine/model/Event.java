// Event.java
package com.company.rulesengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class Event implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("tenantId")
    private String tenantId;
    
    @JsonProperty("userId")
    private String userId;
    
    @JsonProperty("eventType")
    private String eventType;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("properties")
    private Map<String, Object> properties;
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("deviceId")
    private String deviceId;

    // Constructors
    public Event() {}
    
    public Event(String eventId, String tenantId, String userId, String eventType, 
                long timestamp, Map<String, Object> properties) {
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.properties = properties;
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return timestamp == event.timestamp &&
               Objects.equals(eventId, event.eventId) &&
               Objects.equals(tenantId, event.tenantId) &&
               Objects.equals(userId, event.userId) &&
               Objects.equals(eventType, event.eventType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, tenantId, userId, eventType, timestamp);
    }

    @Override
    public String toString() {
        return "Event{" +
               "eventId='" + eventId + '\'' +
               ", tenantId='" + tenantId + '\'' +
               ", userId='" + userId + '\'' +
               ", eventType='" + eventType + '\'' +
               ", timestamp=" + timestamp +
               '}';
    }
}