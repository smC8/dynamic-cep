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