import { useState, useEffect, useCallback, useRef } from 'react';
import { getApiClient } from '@/lib/api';
import type { RunRequest, RunResponse, SubmitRequest, SubmitResponse } from '@/types';

export interface ExecutionResult {
    taskId: string;
    status: 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED';
    runResult?: RunResponse;
    submitResult?: SubmitResponse;
    errorMessage?: string;
    createdAt: string;
    completedAt?: string;
}

interface UseAsyncExecutionOptions {
    onSuccess?: (result: ExecutionResult) => void;
    onError?: (error: Error) => void;
    pollInterval?: number;
    maxPollAttempts?: number;
}

interface UseAsyncExecutionReturn {
    execute: () => Promise<void>;
    result: ExecutionResult | null;
    isPolling: boolean;
    isLoading: boolean;
    error: Error | null;
    reset: () => void;
}

export function useAsyncExecution(
    request: RunRequest | SubmitRequest,
    type: 'run' | 'submit',
    options: UseAsyncExecutionOptions = {}
): UseAsyncExecutionReturn {
    const {
        onSuccess,
        onError,
        pollInterval = 1000,
        maxPollAttempts = 300,
    } = options;

    const [result, setResult] = useState<ExecutionResult | null>(null);
    const [isPolling, setIsPolling] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<Error | null>(null);

    const pollAttempts = useRef(0);
    const pollIntervalId = useRef<number | null>(null);

    const stopPolling = useCallback(() => {
        if (pollIntervalId.current) {
            clearInterval(pollIntervalId.current);
            pollIntervalId.current = null;
        }
        setIsPolling(false);
        pollAttempts.current = 0;
    }, []);

    const pollResult = useCallback(
        async (taskId: string) => {
            try {
                const api = getApiClient();
                const executionResult = await api.getExecutionResult(taskId);

                setResult(executionResult);

                if (executionResult.status === 'SUCCESS' || executionResult.status === 'FAILED') {
                    stopPolling();
                    if (executionResult.status === 'SUCCESS' && onSuccess) {
                        onSuccess(executionResult);
                    } else if (executionResult.status === 'FAILED' && onError) {
                        onError(new Error(executionResult.errorMessage || 'Execution failed'));
                    }
                } else {
                    pollAttempts.current += 1;
                    if (pollAttempts.current >= maxPollAttempts) {
                        stopPolling();
                        const timeoutError = new Error('Polling timeout: result not ready after 5 minutes');
                        setError(timeoutError);
                        if (onError) {
                            onError(timeoutError);
                        }
                    }
                }
            } catch (err) {
                if (err instanceof Error && 'code' in err && (err as any).code === 'NOT_FOUND') {
                    stopPolling();
                    const notFoundError = new Error('Task not found or expired (results are kept for 5 minutes)');
                    setError(notFoundError);
                    if (onError) {
                        onError(notFoundError);
                    }
                } else {
                    console.warn('Polling error, will retry:', err);
                }
            }
        },
        [stopPolling, maxPollAttempts, onSuccess, onError]
    );

    const execute = useCallback(async () => {
        setIsLoading(true);
        setError(null);
        setResult(null);

        try {
            const api = getApiClient();
            let response: { taskId: string };

            if (type === 'run') {
                response = await api.runCodeAsync(request as RunRequest);
            } else {
                response = await api.submitCodeAsync(request as SubmitRequest);
            }

            const { taskId } = response;

            setResult({
                taskId,
                status: 'PENDING',
                createdAt: new Date().toISOString(),
            });

            setIsPolling(true);
            pollAttempts.current = 0;

            await pollResult(taskId);

            pollIntervalId.current = setInterval(() => {
                pollResult(taskId);
            }, pollInterval);
        } catch (err) {
            const execError = err instanceof Error ? err : new Error('Failed to start execution');
            setError(execError);
            if (onError) {
                onError(execError);
            }
        } finally {
            setIsLoading(false);
        }
    }, [request, type, pollResult, pollInterval, onError]);

    const reset = useCallback(() => {
        stopPolling();
        setResult(null);
        setError(null);
        setIsLoading(false);
    }, [stopPolling]);

    useEffect(() => {
        return () => {
            stopPolling();
        };
    }, [stopPolling]);

    return {
        execute,
        result,
        isPolling,
        isLoading,
        error,
        reset,
    };
}
