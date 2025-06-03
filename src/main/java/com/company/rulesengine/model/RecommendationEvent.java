// RecommendationEvent.java
package com.company.rulesengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class RecommendationEvent extends Event {
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("itemId")
    private String itemId;
    
    @JsonProperty("categoryId")
    private String categoryId;
    
    @JsonProperty("userPreferences")
    private Map<String, Object> userPreferences;
    
    @JsonProperty("contextData")
    private Map<String, Object> contextData;
    
    @JsonProperty("previousInteractions")
    private List<String> previousInteractions;
    
    @JsonProperty("location")
    private Location location;

    // Constructors
    public RecommendationEvent() {
        super();
        setEventType("RECOMMENDATION");
    }
    
    public RecommendationEvent(String eventId, String tenantId, String userId, 
                             String itemId, String categoryId) {
        super(eventId, tenantId, userId, "RECOMMENDATION", System.currentTimeMillis(), null);
        this.itemId = itemId;
        this.categoryId = categoryId;
    }

    // Inner class for location data
    public static class Location {
        @JsonProperty("latitude")
        private double latitude;
        
        @JsonProperty("longitude")
        private double longitude;
        
        @JsonProperty("country")
        private String country;
        
        @JsonProperty("city")
        private String city;

        // Constructors
        public Location() {}
        
        public Location(double latitude, double longitude, String country, String city) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.country = country;
            this.city = city;
        }

        // Getters and Setters
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
    }

    // Getters and Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    
    public Map<String, Object> getUserPreferences() { return userPreferences; }
    public void setUserPreferences(Map<String, Object> userPreferences) { this.userPreferences = userPreferences; }
    
    public Map<String, Object> getContextData() { return contextData; }
    public void setContextData(Map<String, Object> contextData) { this.contextData = contextData; }
    
    public List<String> getPreviousInteractions() { return previousInteractions; }
    public void setPreviousInteractions(List<String> previousInteractions) { this.previousInteractions = previousInteractions; }
    
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
}