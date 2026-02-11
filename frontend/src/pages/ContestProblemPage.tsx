import { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useContestBySlug, useContestProblem, useContestSubmissions, useContestSubmit, useContestRegistration, useGetDraft, useSaveDraft } from '@/hooks/useContests';
import { useContestTimer } from '@/hooks/useContestTimer';
import { ContestSubmissionList } from '@/components/contest';
import { CodeEditor } from '@/components/editor/CodeEditor';
import { LanguageSelector } from '@/components/editor/LanguageSelector';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { ArrowLeft, Send, Loader2, Clock, Save } from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import { SUPPORTED_LANGUAGES } from '@/types';

function getLanguageById(id: number): string {
  const lang = SUPPORTED_LANGUAGES.find(l => l.id === id);
  return lang?.monacoLanguage || 'python';
}

const DRAFT_SAVE_INTERVAL = 30_000; // 30 seconds

export default function ContestProblemPage() {
  const { slug, problemId: problemIdParam } = useParams<{ slug: string; problemId: string }>();
  const problemId = parseInt(problemIdParam || '0', 10);

  const [code, setCode] = useState('');
  const [languageId, setLanguageId] = useState(71); // Python 3 default
  const [draftRestored, setDraftRestored] = useState(false);
  const [lastSaved, setLastSaved] = useState<Date | null>(null);
  const codeRef = useRef(code);
  codeRef.current = code;
  const languageIdRef = useRef(languageId);
  languageIdRef.current = languageId;

  const { data: contest, isLoading: contestLoading } = useContestBySlug(slug || '');
  const contestId = contest?.id || 0;

  const { data: problem, isLoading: problemLoading } = useContestProblem(contestId, problemId);
  const { data: submissions, refetch: refetchSubmissions } = useContestSubmissions(contestId, problemId);
  const { data: registration } = useContestRegistration(contestId);
  const { data: draft } = useGetDraft(contestId, problemId);
  const submitMutation = useContestSubmit();
  const saveDraftMutation = useSaveDraft();

  const timerEndTime = contest?.timerMode === 'INDIVIDUAL' && registration?.participantStartTime && contest?.durationMinutes
    ? new Date(new Date(registration.participantStartTime).getTime() + contest.durationMinutes * 60_000).toISOString()
    : contest?.endTime;

  const showTimer = contest?.status === 'RUNNING' && (
    contest.timerMode === 'GLOBAL' || registration?.participantStartTime
  );

  const handleTimeUp = useCallback(() => {
    toast.warning("Time's up! Your latest draft will be auto-submitted for unsolved problems.");
  }, []);

  const { formatted, isLowTime, isCriticalTime, isExpired } = useContestTimer({
    endTime: timerEndTime,
    remainingSeconds: contest?.remainingTimeSeconds,
    onTimeUp: handleTimeUp,
    enabled: !!showTimer,
  });

  useEffect(() => {
    if (draft && !draftRestored) {
      setCode(draft.code);
      setLanguageId(draft.languageId);
      setDraftRestored(true);
    }
  }, [draft, draftRestored]);

  useEffect(() => {
    if (!contestId || !problemId || !showTimer || isExpired) return;

    const interval = setInterval(() => {
      const currentCode = codeRef.current;
      if (currentCode.trim()) {
        saveDraftMutation.mutate(
          { contestId, problemId, code: currentCode, languageId: languageIdRef.current },
          { onSuccess: () => setLastSaved(new Date()) }
        );
      }
    }, DRAFT_SAVE_INTERVAL);

    return () => clearInterval(interval);
  }, [contestId, problemId, showTimer, isExpired, saveDraftMutation]);

  const isLoading = contestLoading || problemLoading;

  const handleSubmit = async () => {
    if (!contest || !problem || !code.trim()) {
      toast.error('Please write some code before submitting');
      return;
    }

    try {
      await submitMutation.mutateAsync({
        contestId: contest.id,
        data: { problemId: problem.problemId, code, languageId },
      });
      toast.success('Solution submitted successfully');
      refetchSubmissions();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to submit');
    }
  };

  const handleManualSave = () => {
    if (!contestId || !problemId || !code.trim()) return;
    saveDraftMutation.mutate(
      { contestId, problemId, code, languageId },
      { onSuccess: () => { setLastSaved(new Date()); toast.success('Draft saved'); } }
    );
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-8 px-4">
        <Skeleton className="h-8 w-1/4 mb-4" />
        <Skeleton className="h-64 w-full mb-4" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (!contest || !problem) {
    return (
      <div className="container mx-auto py-8 px-4 text-center">
        <p className="text-destructive mb-4">Problem not found</p>
        <Button asChild>
          <Link to={`/contests/${slug}`}>Back to Contest</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <Button asChild variant="ghost" size="sm">
            <Link to={`/contests/${slug}`}>
              <ArrowLeft className="h-4 w-4 mr-1" />
              Back
            </Link>
          </Button>
          <h1 className="text-2xl font-bold">{problem.title}</h1>
          <Badge variant="secondary">{problem.pointValue} pts</Badge>
        </div>

        {showTimer && (
          <div className={cn(
            'flex items-center gap-2 px-4 py-2 rounded-lg font-mono text-lg',
            isCriticalTime && 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 animate-pulse',
            isLowTime && !isCriticalTime && 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400',
            !isLowTime && !isExpired && 'bg-muted text-foreground',
            isExpired && 'bg-red-200 text-red-800 dark:bg-red-900/50 dark:text-red-300',
          )}>
            <Clock className="h-4 w-4" />
            {isExpired ? "Time's Up!" : formatted}
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Problem Statement</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="prose dark:prose-invert max-w-none">
                {problem.statement}
              </div>
            </CardContent>
          </Card>

          {problem.constraints && (
            <Card>
              <CardHeader>
                <CardTitle>Constraints</CardTitle>
              </CardHeader>
              <CardContent>
                <pre className="text-sm bg-muted p-4 rounded-lg overflow-x-auto">
                  {problem.constraints}
                </pre>
              </CardContent>
            </Card>
          )}

          {problem.examples && problem.examples.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Examples</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {problem.examples.map((example, index) => (
                  <div key={index} className="space-y-2">
                    <div>
                      <div className="text-sm font-medium mb-1">Input:</div>
                      <pre className="text-sm bg-muted p-3 rounded-lg overflow-x-auto">
                        {example.input}
                      </pre>
                    </div>
                    <div>
                      <div className="text-sm font-medium mb-1">Output:</div>
                      <pre className="text-sm bg-muted p-3 rounded-lg overflow-x-auto">
                        {example.expectedOutput}
                      </pre>
                    </div>
                  </div>
                ))}
              </CardContent>
            </Card>
          )}
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle>Solution</CardTitle>
                <div className="flex items-center gap-2">
                  {lastSaved && (
                    <span className="text-xs text-muted-foreground">
                      Saved {lastSaved.toLocaleTimeString()}
                    </span>
                  )}
                  <LanguageSelector value={languageId} onChange={setLanguageId} />
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <CodeEditor
                value={code}
                onChange={setCode}
                language={getLanguageById(languageId)}
              />
              <div className="flex justify-between mt-4">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleManualSave}
                  disabled={saveDraftMutation.isPending || !code.trim()}
                >
                  {saveDraftMutation.isPending ? (
                    <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                  ) : (
                    <Save className="h-4 w-4 mr-1" />
                  )}
                  Save Draft
                </Button>
                <Button
                  onClick={handleSubmit}
                  disabled={submitMutation.isPending || !contest.canSubmit || isExpired}
                >
                  {submitMutation.isPending ? (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  ) : (
                    <Send className="h-4 w-4 mr-2" />
                  )}
                  Submit
                </Button>
              </div>
            </CardContent>
          </Card>

          {submissions && submissions.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Your Submissions</CardTitle>
              </CardHeader>
              <CardContent>
                <ContestSubmissionList submissions={submissions} />
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </div>
  );
}
