package com.codearena.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ContestSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(ContestSchedulerJob.class);

    private final ContestLifecycleService lifecycleService;

    public ContestSchedulerJob(ContestLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    @Scheduled(fixedRate = 60000) // Every minute (60,000 milliseconds)
    public void processContestTransitions() {
        log.debug("Processing scheduled contest transitions");
        try {
            lifecycleService.processScheduledTransitions();
        } catch (Exception e) {
            log.error("Error processing contest transitions: {}", e.getMessage(), e);
        }
    }
}
