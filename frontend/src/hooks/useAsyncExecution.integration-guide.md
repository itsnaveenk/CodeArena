/**
 * Example integration of useAsyncExecution hook into ProblemSolvePage.
 * 
 * This demonstrates how to replace the synchronous execution hooks with
 * the async polling-based implementation for better scalability.
 * 
 * Key changes from the original ProblemSolvePage.tsx:
 * 1. Replace useRunCode/useSubmitCode with useAsyncExecution
 * 2. Update ExecutionPanel to show polling states (PENDING/PROCESSING)
 * 3. Add timeout/expiration handling
 * 
 * Original imports:
 * - import { useRunCode, useSubmitCode } from '@/hooks/useSubmission';
 * 
 * New imports:
 * - import { useAsyncExecution } from '@/hooks/useAsyncExecution';
 * 
 * ---
 * 
 * OPTION 1: Minimal Integration (Recommended for testing)
 * Replace the run/submit handlers with async execution:
 */

import { useState } from 'react';
import { useAsyncExecution } from '@/hooks/useAsyncExecution';
import { toast } from '@/lib/toast';
import type { RunRequest, SubmitRequest } from '@/types';

// In your component:
function ProblemSolvePageAsync() {
    const [runRequest, setRunRequest] = useState<RunRequest | null>(null);
    const [submitRequest, setSubmitRequest] = useState<SubmitRequest | null>(null);

    // Create async execution hooks
    const runExecution = useAsyncExecution(
        runRequest || { problemId: 0, code: '', languageId: 0 },
        'run',
        {
            onSuccess: (result) => {
                if (result.runResult) {
                    // Handle run success - display results
                    toast.success('Code executed successfully');
                }
            },
            onError: (error) => {
                toast.error(error.message);
            },
        }
    );

    const submitExecution = useAsyncExecution(
        submitRequest || { problemId: 0, code: '', languageId: 0 },
        'submit',
        {
            onSuccess: (result) => {
                if (result.submitResult?.verdict === 'ACCEPTED') {
                    toast.success('Accepted! All test cases passed.');
                } else if (result.submitResult) {
                    toast.error(result.submitResult.verdict.replace(/_/g, ' '));
                }
            },
            onError: (error) => {
                toast.error(error.message);
            },
        }
    );

    const handleRun = () => {
        if (!problem || !code.trim()) {
            toast.error('Please write some code first');
            return;
        }
        setRunRequest({ problemId: problem.id, languageId, code });
        runExecution.execute();
    };

    const handleSubmit = () => {
        if (!problem || !code.trim()) {
            toast.error('Please write some code first');
            return;
        }
        setSubmitRequest({ problemId: problem.id, languageId, code });
        submitExecution.execute();
    };

    // Update UI to show polling status
    const isRunning = runExecution.isLoading || runExecution.isPolling;
    const isSubmitting = submitExecution.isLoading || submitExecution.isPolling;

    // Pass results to ExecutionPanel
    return (
        <ExecutionPanel
      runResult= { runExecution.result?.runResult || null }
    submitResult = { submitExecution.result?.submitResult || null }
    isRunning = { isRunning }
    isSubmitting = { isSubmitting }
        />
  );
}

/**
 * ---
 * 
 * OPTION 2: Enhanced ExecutionPanel with Polling States
 * Update ExecutionPanel to show more detailed status during async execution:
 */

import { Loader2 } from 'lucide-react';
import type { ExecutionResult } from '@/hooks/useAsyncExecution';

interface EnhancedExecutionPanelProps {
    executionResult?: ExecutionResult | null;
    isPolling?: boolean;
}

function EnhancedExecutionPanel({ executionResult, isPolling }: EnhancedExecutionPanelProps) {
    if (!executionResult) {
        return (
            <div className= "p-4 text-center text-muted-foreground" >
            Run your code to see results here.
      </div>
    );
    }

    // Show polling states
    if (executionResult.status === 'PENDING' || executionResult.status === 'PROCESSING') {
        return (
            <div className= "p-4 text-center" >
            <div className="flex items-center justify-center gap-2 text-muted-foreground" >
                <Loader2 className="h-5 w-5 animate-spin" />
                    <span>
                    { executionResult.status === 'PENDING' ? 'Queued...' : 'Executing your code...' }
                    </span>
                    </div>
                    < p className = "text-xs text-muted-foreground mt-2" >
                        Task ID: { executionResult.taskId }
        </p>
            </div>
    );
    }

    // Show errors
    if (executionResult.status === 'FAILED') {
        return (
            <div className= "p-4" >
            <div className="text-red-500 font-medium mb-2" > Execution Failed </div>
                < div className = "text-sm text-muted-foreground" >
                    { executionResult.errorMessage || 'An error occurred during execution' }
                    </div>
                    </div>
    );
    }

    // Show success results
    if (executionResult.status === 'SUCCESS') {
        const { runResult, submitResult } = executionResult;

        // Render run or submit results (use existing ExecutionPanel logic)
        if (submitResult) {
            return (
                <div className= "p-4 space-y-4" >
                <VerdictBadge verdict={ submitResult.verdict } />
            {/* ... rest of submit result display ... */ }
            </div>
      );
        }

        if (runResult) {
            return (
                <div className= "p-4 space-y-4" >
                {/* ... run result display ... */ }
                </div>
      );
        }
    }

    return null;
}

/**
 * ---
 * 
 * OPTION 3: Gradual Migration Strategy
 * Keep both sync and async options, use feature flag or user preference:
 */

function ProblemSolvePageHybrid() {
    const [useAsyncExecution, setUseAsyncExecution] = useState(true); // Feature flag

    if (useAsyncExecution) {
        // Use async execution hooks
        const runExecution = useAsyncExecution(runRequest, 'run', { ... });
        // ... async implementation
    } else {
        // Use legacy sync hooks
        const runMutation = useRunCode();
        // ... sync implementation
    }
}

/**
 * ---
 * 
 * MIGRATION CHECKLIST:
 * 
 * 1. Backend: ✅ Complete
 *    - Async endpoints created
 *    - Redis Stream consumer running
 *    - Result polling endpoint ready
 * 
 * 2. Frontend Foundation: ✅ Complete
 *    - API client methods added
 *    - useAsyncExecution hook created
 * 
 * 3. UI Integration: 🔄 In Progress
 *    - [ ] Update ProblemSolvePage.tsx to use useAsyncExecution
 *    - [ ] Update ContestProblemPage.tsx for contest submissions
 *    - [ ] Add polling UI indicators (Loader2 icon, status text)
 *    - [ ] Test with local Redis instance
 * 
 * 4. Testing:
 *    - [ ] Test run code with async endpoint
 *    - [ ] Test submit code with async endpoint
 *    - [ ] Test timeout handling (5 min)
 *    - [ ] Test task expiration (404 after TTL)
 *    - [ ] Test error scenarios
 * 
 * ---
 * 
 * IMPLEMENTATION NOTES:
 * 
 * 1. The useAsyncExecution hook automatically handles:
 *    - Initial API call to start execution
 *    - Polling every 1 second for results
 *    - Timeout after 5 minutes
 *    - Task expiration detection (404)
 *    - Cleanup on component unmount
 * 
 * 2. Status flow:
 *    PENDING (queued) → PROCESSING (executing) → SUCCESS/FAILED
 * 
 * 3. The hook provides:
 *    - `execute()`: Function to trigger execution
 *    - `result`: Current execution result with status
 *    - `isPolling`: Boolean indicating if actively polling
 *    - `isLoading`: Boolean for initial API call
 *    - `error`: Error object if failed
 *    - `reset()`: Reset state for new execution
 * 
 * 4. For best UX:
 *    - Show spinner during isLoading || isPolling
 *    - Display taskId for debugging
 *    - Show estimated time (based on polling interval)
 *    - Disable Run/Submit buttons while polling
 */

export { };
