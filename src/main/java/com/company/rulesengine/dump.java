// ===== RULES PACKAGE =====

// RuleManager.java
package com.company.rulesengine.rules;

import com.company.rulesengine.config.ApplicationConfig;
import com.company.rulesengine.model.Rule;
import com.company.rulesengine.cache.RuleCacheManager;
import com.company.rulesengine.metrics.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RuleManager {
    private static final Logger logger = LoggerFactory.getLogger(RuleManager.class);
    
    private final ApplicationConfig config;
    private final RuleRepository ruleRepository;
    private final RuleCacheManager cacheManager;
    private final DynamicRuleLoader dynamicLoader;
    private final RuleVersionManager versionManager;
    private final MetricsCollector metricsCollector;
    private final ScheduledExecutorService scheduler;
    
    private final ConcurrentHashMap<String, Long> tenantRuleVersions;

    public RuleManager(ApplicationConfig config) {
        this.config = config;
        this.ruleRepository = new RuleRepository(config);
        this.cacheManager = new RuleCacheManager(config);
        this.dynamicLoader = new DynamicRuleLoader(config, this);
        this.versionManager = new RuleVersionManager(config);
        this.metricsCollector = new MetricsCollector();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.tenantRuleVersions = new ConcurrentHashMap<>();
        
        initializeRuleManager();
    }

    private void initializeRuleManager() {
        logger.info("Initializing Rule Manager");
        
        // Start periodic rule refresh
        scheduler.scheduleAtFixedRate(
            this::refreshRules, 
            config.getRuleRefreshInterval(), 
            config.getRuleRefreshInterval(), 
            TimeUnit.SECONDS
        );
        
        // Start rule cache cleanup
        scheduler.scheduleAtFixedRate(
            cacheManager::cleanup, 
            config.getCacheCleanupInterval(), 
            config.getCacheCleanupInterval(), 
            TimeUnit.SECONDS
        );
        
        logger.info("Rule Manager initialized successfully");
    }

    public List<Rule> getActiveRulesForTenant(String tenantId) {
        try {
            // First check cache
            List<Rule> cachedRules = cacheManager.getRulesForTenant(tenantId);
            if (cachedRules != null && !cachedRules.isEmpty()) {
                metricsCollector.incrementCounter("rule_cache_hit", tenantId);
                return cachedRules;
            }
            
            // Load from repository if not in cache
            metricsCollector.incrementCounter("rule_cache_miss", tenantId);
            List<Rule> rules = ruleRepository.getActiveRulesForTenant(tenantId);
            
            // Cache the rules
            cacheManager.cacheRulesForTenant(tenantId, rules);
            
            return rules;
            
        } catch (Exception e) {
            logger.error("Error getting rules for tenant {}: {}", tenantId, e.getMessage(), e);
            metricsCollector.incrementCounter("rule_fetch_errors", tenantId);
            return java.util.Collections.emptyList();
        }
    }

    public CompletableFuture<Boolean> updateRule(Rule rule) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Validate rule
                if (!validateRule(rule)) {
                    logger.warn("Rule validation failed for rule {}", rule.getId());
                    return false;
                }
                
                // Update version
                rule = versionManager.createNewVersion(rule);
                
                // Save to repository
                boolean saved = ruleRepository.saveRule(rule);
                if (!saved) {
                    logger.error("Failed to save rule {}", rule.getId());
                    return false;
                }
                
                // Invalidate cache for tenant
                cacheManager.invalidateTenantCache(rule.getTenantId());
                
                // Update version tracking
                tenantRuleVersions.put(rule.getTenantId(), System.currentTimeMillis());
                
                logger.info("Rule {} updated successfully for tenant {}", rule.getId(), rule.getTenantId());
                metricsCollector.incrementCounter("rules_updated", rule.getTenantId());
                
                return true;
                
            } catch (Exception e) {
                logger.error("Error updating rule {}: {}", rule.getId(), e.getMessage(), e);
                metricsCollector.incrementCounter("rule_update_errors", rule.getTenantId());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> deleteRule(String ruleId, String tenantId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                boolean deleted = ruleRepository.deleteRule(ruleId, tenantId);
                if (deleted) {
                    cacheManager.invalidateTenantCache(tenantId);
                    tenantRuleVersions.put(tenantId, System.currentTimeMillis());
                    logger.info("Rule {} deleted successfully for tenant {}", ruleId, tenantId);
                    metricsCollector.incrementCounter("rules_deleted", tenantId);
                }
                return deleted;
                
            } catch (Exception e) {
                logger.error("Error deleting rule {}: {}", ruleId, e.getMessage(), e);
                metricsCollector.incrementCounter("rule_delete_errors", tenantId);
                return false;
            }
        });
    }

    public Rule getRule(String ruleId, String tenantId) {
        try {
            Rule cachedRule = cacheManager.getRule(ruleId, tenantId);
            if (cachedRule != null) {
                metricsCollector.incrementCounter("rule_cache_hit", tenantId);
                return cachedRule;
            }
            
            metricsCollector.incrementCounter("rule_cache_miss", tenantId);
            Rule rule = ruleRepository.getRule(ruleId, tenantId);
            
            if (rule != null) {
                cacheManager.cacheRule(rule);
            }
            
            return rule;
            
        } catch (Exception e) {
            logger.error("Error getting rule {} for tenant {}: {}", ruleId, tenantId, e.getMessage(), e);
            metricsCollector.incrementCounter("rule_fetch_errors", tenantId);
            return null;
        }
    }

    public int getRuleCountForTenant(String tenantId) {
        try {
            return ruleRepository.getRuleCountForTenant(tenantId);
        } catch (Exception e) {
            logger.error("Error getting rule count for tenant {}: {}", tenantId, e.getMessage(), e);
            return 0;
        }
    }

    private void refreshRules() {
        try {
            logger.debug("Starting periodic rule refresh");
            
            // Get all active tenants
            List<String> activeTenants = ruleRepository.getActiveTenants();
            
            // FlinkRulesEngineApplication.java
package com.company.rulesengine;

import com.company.rulesengine.config.ApplicationConfig;
import com.company.rulesengine.core.RulesEngineJob;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlinkRulesEngineApplication {
    private static final Logger logger = LoggerFactory.getLogger(FlinkRulesEngineApplication.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting Flink Dynamic Rules Engine Application");
        
        ParameterTool parameters = ParameterTool.fromArgs(args);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // Make parameters available globally
        env.getConfig().setGlobalJobParameters(parameters);
        
        // Initialize application configuration
        ApplicationConfig config = new ApplicationConfig(parameters);
        
        // Create and execute the rules engine job
        RulesEngineJob job = new RulesEngineJob(config);
        job.execute(env);
        
        logger.info("Flink Dynamic Rules Engine Application started successfully");
    }
}

// ===== CORE PACKAGE =====

// RulesEngineJob.java
package com.company.rulesengine.core;

import com.company.rulesengine.config.ApplicationConfig;
import com.company.rulesengine.model.Event;
import com.company.rulesengine.model.RuleResult;
import com.company.rulesengine.stream.sources.KafkaEventSource;
import com.company.rulesengine.stream.processors.EventProcessor;
import com.company.rulesengine.stream.sinks.KafkaResultSink;
import com.company.rulesengine.tenant.TenantManager;
import com.company.rulesengine.rules.RuleManager;
import com.company.rulesengine.state.StateManager;
import com.company.rulesengine.metrics.MetricsCollector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RulesEngineJob {
    private static final Logger logger = LoggerFactory.getLogger(RulesEngineJob.class);
    
    private final ApplicationConfig config;
    private final TenantManager tenantManager;
    private final RuleManager ruleManager;
    private final StateManager stateManager;
    private final MetricsCollector metricsCollector;

    public RulesEngineJob(ApplicationConfig config) {
        this.config = config;
        this.tenantManager = new TenantManager(config);
        this.ruleManager = new RuleManager(config);
        this.stateManager = new StateManager(config);
        this.metricsCollector = new MetricsCollector();
    }

    public void execute(StreamExecutionEnvironment env) throws Exception {
        logger.info("Setting up Flink Rules Engine Job");
        
        // Configure environment
        configureEnvironment(env);
        
        // Create event source
        KafkaEventSource eventSource = new KafkaEventSource(config);
        DataStream<Event> eventStream = eventSource.createSource(env);
        
        // Process events through multi-tenant rule processor
        MultiTenantRuleProcessor processor = new MultiTenantRuleProcessor(
            ruleManager, tenantManager, stateManager, metricsCollector);
        
        DataStream<RuleResult> resultStream = eventStream
            .keyBy(Event::getTenantId)
            .process(processor)
            .name("Multi-Tenant Rule Processor");
        
        // Create result sink
        KafkaResultSink resultSink = new KafkaResultSink(config);
        resultSink.addSink(resultStream);
        
        // Execute the job
        env.execute("Dynamic Rules Engine - Multi-Tenant");
    }
    
    private void configureEnvironment(StreamExecutionEnvironment env) {
        env.enableCheckpointing(config.getCheckpointInterval());
        env.setMaxParallelism(config.getMaxParallelism());
        env.setParallelism(config.getParallelism());
        
        // Configure state backend
        stateManager.configureStateBackend(env);
        
        logger.info("Environment configured with parallelism: {}, checkpoint interval: {}", 
                   config.getParallelism(), config.getCheckpointInterval());
    }
}

// MultiTenantRuleProcessor.java
package com.company.rulesengine.core;

import com.company.rulesengine.model.Event;
import com.company.rulesengine.model.Rule;
import com.company.rulesengine.model.RuleResult;
import com.company.rulesengine.model.Tenant;
import com.company.rulesengine.rules.RuleManager;
import com.company.rulesengine.tenant.TenantManager;
import com.company.rulesengine.tenant.TenantContext;
import com.company.rulesengine.state.StateManager;
import com.company.rulesengine.metrics.MetricsCollector;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MultiTenantRuleProcessor extends KeyedProcessFunction<String, Event, RuleResult> {
    private static final Logger logger = LoggerFactory.getLogger(MultiTenantRuleProcessor.class);
    
    private final RuleManager ruleManager;
    private final TenantManager tenantManager;
    private final StateManager stateManager;
    private final MetricsCollector metricsCollector;
    
    private transient RuleExecutor ruleExecutor;

    public MultiTenantRuleProcessor(RuleManager ruleManager, TenantManager tenantManager, 
                                  StateManager stateManager, MetricsCollector metricsCollector) {
        this.ruleManager = ruleManager;
        this.tenantManager = tenantManager;
        this.stateManager = stateManager;
        this.metricsCollector = metricsCollector;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.ruleExecutor = new RuleExecutor(ruleManager, stateManager, metricsCollector);
        logger.info("MultiTenantRuleProcessor initialized");
    }

    @Override
    public void processElement(Event event, Context context, Collector<RuleResult> collector) throws Exception {
        long startTime = System.currentTimeMillis();
        String tenantId = event.getTenantId();
        
        try {
            // Set tenant context
            TenantContext tenantContext = tenantManager.getTenantContext(tenantId);
            if (tenantContext == null) {
                logger.warn("Unknown tenant: {}", tenantId);
                metricsCollector.incrementCounter("unknown_tenant", tenantId);
                return;
            }
            
            // Get active rules for tenant
            List<Rule> tenantRules = ruleManager.getActiveRulesForTenant(tenantId);
            
            if (tenantRules.isEmpty()) {
                metricsCollector.incrementCounter("no_rules", tenantId);
                return;
            }
            
            // Execute rules asynchronously for better performance
            CompletableFuture<List<RuleResult>> futureResults = ruleExecutor.executeRulesAsync(
                event, tenantRules, tenantContext, context);
            
            // Collect results
            List<RuleResult> results = futureResults.get();
            for (RuleResult result : results) {
                collector.collect(result);
            }
            
            // Record metrics
            long processingTime = System.currentTimeMillis() - startTime;
            metricsCollector.recordProcessingTime(tenantId, processingTime);
            metricsCollector.incrementCounter("events_processed", tenantId);
            
        } catch (Exception e) {
            logger.error("Error processing event for tenant {}: {}", tenantId, e.getMessage(), e);
            metricsCollector.incrementCounter("processing_errors", tenantId);
            
            // Create error result
            RuleResult errorResult = RuleResult.createError(event, e.getMessage());
            collector.collect(errorResult);
        }
    }
}

// RuleExecutor.java
package com.company.rulesengine.core;

import com.company.rulesengine.model.Event;
import com.company.rulesengine.model.Rule;
import com.company.rulesengine.model.RuleResult;
import com.company.rulesengine.rules.RuleManager;
import com.company.rulesengine.rules.engine.RuleEngine;
import com.company.rulesengine.tenant.TenantContext;
import com.company.rulesengine.state.StateManager;
import com.company.rulesengine.metrics.MetricsCollector;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class RuleExecutor {
    private static final Logger logger = LoggerFactory.getLogger(RuleExecutor.class);
    
    private final RuleManager ruleManager;
    private final StateManager stateManager;
    private final MetricsCollector metricsCollector;
    private final ExecutorService executorService;
    private final RuleEngine ruleEngine;

    public RuleExecutor(RuleManager ruleManager, StateManager stateManager, MetricsCollector metricsCollector) {
        this.ruleManager = ruleManager;
        this.stateManager = stateManager;
        this.metricsCollector = metricsCollector;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.ruleEngine = new RuleEngine();
    }

    public CompletableFuture<List<RuleResult>> executeRulesAsync(Event event, List<Rule> rules, 
                                                               TenantContext tenantContext, 
                                                               KeyedProcessFunction.Context context) {
        
        List<CompletableFuture<RuleResult>> futures = rules.stream()
            .map(rule -> executeRuleAsync(event, rule, tenantContext, context))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(result -> result != null)
                .collect(Collectors.toList()));
    }

    private CompletableFuture<RuleResult> executeRuleAsync(Event event, Rule rule, 
                                                         TenantContext tenantContext,
                                                         KeyedProcessFunction.Context context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // Execute rule
                boolean ruleMatched = ruleEngine.evaluate(rule, event, tenantContext);
                
                if (ruleMatched) {
                    RuleResult result = RuleResult.builder()
                        .event(event)
                        .rule(rule)
                        .tenantId(event.getTenantId())
                        .matched(true)
                        .timestamp(System.currentTimeMillis())
                        .processingTime(System.currentTimeMillis() - startTime)
                        .build();
                    
                    metricsCollector.incrementCounter("rule_matches", event.getTenantId());
                    return result;
                }
                
                return null;
                
            } catch (Exception e) {
                logger.error("Error executing rule {} for tenant {}: {}", 
                           rule.getId(), event.getTenantId(), e.getMessage(), e);
                metricsCollector.incrementCounter("rule_execution_errors", event.getTenantId());
                return RuleResult.createError(event, rule, e.getMessage());
            }
        }, executorService);
    }

    public void close() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}

// RuleEvaluator.java
package com.company.rulesengine.core;

import com.company.rulesengine.model.Event;
import com.company.rulesengine.model.Rule;
import com.company.rulesengine.tenant.TenantContext;
import com.company.rulesengine.rules.engine.ExpressionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class RuleEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(RuleEvaluator.class);
    
    private final ExpressionEvaluator expressionEvaluator;

    public RuleEvaluator() {
        this.expressionEvaluator = new ExpressionEvaluator();
    }

    public boolean evaluate(Rule rule, Event event, TenantContext tenantContext) {
        try {
            // Create evaluation context
            Map<String, Object> context = createEvaluationContext(event, rule, tenantContext);
            
            // Evaluate rule expression
            return expressionEvaluator.evaluate(rule.getExpression(), context);
            
        } catch (Exception e) {
            logger.error("Error evaluating rule {}: {}", rule.getId(), e.getMessage(), e);
            return false;
        }
    }

    private Map<String, Object> createEvaluationContext(Event event, Rule rule, TenantContext tenantContext) {
        Map<String, Object> context = new java.util.HashMap<>();
        
        // Add event properties
        context.put("event", event);
        context.put("eventType", event.getEventType());
        context.put("userId", event.getUserId());
        context.put("timestamp", event.getTimestamp());
        context.put("properties", event.getProperties());
        
        // Add tenant context
        context.put("tenantId", tenantContext.getTenantId());
        context.put("tenantConfig", tenantContext.getConfiguration());
        
        // Add rule metadata
        context.put("ruleId", rule.getId());
        context.put("ruleType", rule.getType());
        
        return context;
    }
}

// RuleResultAggregator.java
package com.company.rulesengine.core;

import com.company.rulesengine.model.RuleResult;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleResultAggregator implements WindowFunction<RuleResult, RuleResult, String, TimeWindow> {
    private static final Logger logger = LoggerFactory.getLogger(RuleResultAggregator.class);

    @Override
    public void apply(String tenantId, TimeWindow window, Iterable<RuleResult> results, 
                     Collector<RuleResult> collector) throws Exception {
        
        List<RuleResult> resultList = new ArrayList<>();
        results.forEach(resultList::add);
        
        if (resultList.isEmpty()) {
            return;
        }
        
        // Group results by rule type
        Map<String, List<RuleResult>> groupedResults = resultList.stream()
            .collect(Collectors.groupingBy(result -> result.getRule().getType()));
        
        // Create aggregated results
        for (Map.Entry<String, List<RuleResult>> entry : groupedResults.entrySet()) {
            String ruleType = entry.getKey();
            List<RuleResult> typeResults = entry.getValue();
            
            RuleResult aggregatedResult = RuleResult.builder()
                .tenantId(tenantId)
                .ruleType(ruleType)
                .windowStart(window.getStart())
                .windowEnd(window.getEnd())
                .matchCount(typeResults.size())
                .averageProcessingTime(calculateAverageProcessingTime(typeResults))
                .build();
            
            collector.collect(aggregatedResult);
        }
        
        logger.debug("Aggregated {} results for tenant {} in window [{}, {}]", 
                    resultList.size(), tenantId, window.getStart(), window.getEnd());
    }
    
    private long calculateAverageProcessingTime(List<RuleResult> results) {
        return (long) results.stream()
            .mapToLong(RuleResult::getProcessingTime)
            .average()
            .orElse(0.0);
    }
}

// ===== MODEL PACKAGE =====

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