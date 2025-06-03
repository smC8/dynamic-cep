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