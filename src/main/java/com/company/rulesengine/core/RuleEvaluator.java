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

