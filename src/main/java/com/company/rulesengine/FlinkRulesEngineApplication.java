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

