package com.codearena.service;

public interface ContestLifecycleService {

    void processScheduledTransitions();

    void transitionToRunning(Long contestId);

    void transitionToFinished(Long contestId);

    void finalizeContest(Long contestId);
}
