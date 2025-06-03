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