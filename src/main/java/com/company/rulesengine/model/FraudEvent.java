// FraudEvent.java
package com.company.rulesengine.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class FraudEvent extends Event {
    private static final long serialVersionUID = 1L;
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("amount")
    private double amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("merchantId")
    private String merchantId;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("ipAddress")
    private String ipAddress;
    
    @JsonProperty("userAgent")
    private String userAgent;
    
    @JsonProperty("deviceFingerprint")
    private String deviceFingerprint;
    
    @JsonProperty("riskScore")
    private double riskScore;
    
    @JsonProperty("geoLocation")
    private GeoLocation geoLocation;
    
    @JsonProperty("transactionMetadata")
    private Map<String, Object> transactionMetadata;

    // Constructors
    public FraudEvent() {
        super();
        setEventType("FRAUD_DETECTION");
    }
    
    public FraudEvent(String eventId, String tenantId, String userId, 
                     String transactionId, double amount, String currency) {
        super(eventId, tenantId, userId, "FRAUD_DETECTION", System.currentTimeMillis(), null);
        this.transactionId = transactionId;
        this.amount = amount;
        this.currency = currency;
    }

    // Inner class for geo location
    public static class GeoLocation {
        @JsonProperty("latitude")
        private double latitude;
        
        @JsonProperty("longitude")
        private double longitude;
        
        @JsonProperty("country")
        private String country;
        
        @JsonProperty("city")
        private String city;
        
        @JsonProperty("region")
        private String region;
        
        @JsonProperty("isp")
        private String isp;

        // Constructors
        public GeoLocation() {}
        
        public GeoLocation(double latitude, double longitude, String country, String city) {
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
        
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        
        public String getIsp() { return isp; }
        public void setIsp(String isp) { this.isp = isp; }
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    
    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
    
    public GeoLocation getGeoLocation() { return geoLocation; }
    public void setGeoLocation(GeoLocation geoLocation) { this.geoLocation = geoLocation; }
    
    public Map<String, Object> getTransactionMetadata() { return transactionMetadata; }
    public void setTransactionMetadata(Map<String, Object> transactionMetadata) { this.transactionMetadata = transactionMetadata; }
}