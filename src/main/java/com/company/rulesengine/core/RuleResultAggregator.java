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

