import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { ScrollArea } from '@/components/ui/scroll-area';
import { DifficultyBadge } from '@/components/problem/DifficultyBadge';
import { toast } from '@/lib/toast';
import { Check, X, Eye, EyeOff, Code, User } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

interface ProblemReviewDialogProps {
  problemId: number | null;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}

export function ProblemReviewDialog({
  problemId,
  open,
  onOpenChange,
  onSuccess,
}: ProblemReviewDialogProps) {
  const [rejectionReason, setRejectionReason] = useState('');
  const [showRejectInput, setShowRejectInput] = useState(false);
  const queryClient = useQueryClient();

  const { data: problem, isLoading } = useQuery({
    queryKey: ['admin', 'problem-review', problemId],
    queryFn: () => getApiClient().getProblemForReview(problemId!),
    enabled: !!problemId && open,
  });

  const publishMutation = useMutation({
    mutationFn: (id: number) => getApiClient().publishProblem(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'pending'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'problems'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
      toast.success('Problem published successfully');
      onOpenChange(false);
      onSuccess?.();
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to publish problem'),
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, reason }: { id: number; reason: string }) =>
      getApiClient().rejectProblem(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'pending'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'problems'] });
      queryClient.invalidateQueries({ queryKey: ['admin', 'stats'] });
      toast.success('Problem rejected');
      setRejectionReason('');
      setShowRejectInput(false);
      onOpenChange(false);
      onSuccess?.();
    },
    onError: (error) => toast.error(error instanceof Error ? error.message : 'Failed to reject problem'),
  });

  const handlePublish = () => {
    if (problemId) {
      publishMutation.mutate(problemId);
    }
  };

  const handleReject = () => {
    if (!rejectionReason.trim()) {
      toast.error('Please provide a rejection reason');
      return;
    }
    if (rejectionReason.trim().length < 10) {
      toast.error('Rejection reason must be at least 10 characters');
      return;
    }
    if (problemId) {
      rejectMutation.mutate({ id: problemId, reason: rejectionReason.trim() });
    }
  };

  if (isLoading || !problem) {
    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-5xl max-h-[90vh]">
          <DialogHeader>
            <Skeleton className="h-8 w-64" />
            <Skeleton className="h-4 w-96 mt-2" />
          </DialogHeader>
          <div className="space-y-4">
            <Skeleton className="h-32 w-full" />
            <Skeleton className="h-32 w-full" />
          </div>
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-5xl max-h-[90vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-3">
            <span>{problem.title}</span>
            <DifficultyBadge difficulty={problem.difficulty} />
          </DialogTitle>
          <DialogDescription className="flex items-center gap-2 text-sm">
            <User className="h-4 w-4" />
            <span>
              Created by {problem.authorName} ({problem.authorEmail})
            </span>
          </DialogDescription>
        </DialogHeader>

        <ScrollArea className="flex-1 pr-4">
          <Tabs defaultValue="overview" className="w-full">
            <TabsList className="grid w-full grid-cols-4">
              <TabsTrigger value="overview">Overview</TabsTrigger>
              <TabsTrigger value="problem">Problem</TabsTrigger>
              <TabsTrigger value="testcases">
                Test Cases ({problem.testcaseCount})
              </TabsTrigger>
              <TabsTrigger value="starter">Starter Code</TabsTrigger>
            </TabsList>

            <TabsContent value="overview" className="space-y-4 mt-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <h3 className="font-semibold">Statistics</h3>
                  <div className="text-sm space-y-1">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Submissions:</span>
                      <span className="font-medium">{problem.submissionCount}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Total Test Cases:</span>
                      <span className="font-medium">{problem.testcaseCount}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Visible:</span>
                      <span className="font-medium text-green-600">{problem.visibleTestcaseCount}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Hidden:</span>
                      <span className="font-medium text-blue-600">{problem.hiddenTestcaseCount}</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <h3 className="font-semibold">Metadata</h3>
                  <div className="text-sm space-y-1">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Created:</span>
                      <span className="font-medium">
                        {new Date(problem.createdAt).toLocaleDateString()}
                      </span>
                    </div>
                    {problem.updatedAt && (
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Updated:</span>
                        <span className="font-medium">
                          {new Date(problem.updatedAt).toLocaleDateString()}
                        </span>
                      </div>
                    )}
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Tags:</span>
                      <div className="flex gap-1 flex-wrap">
                        {problem.tags.map((tag) => (
                          <Badge key={tag} variant="outline" className="text-xs">
                            {tag}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </TabsContent>

            <TabsContent value="problem" className="mt-4">
              <div className="prose dark:prose-invert max-w-none">
                <ReactMarkdown
                  components={{
                    code({ className, children, ...props }: any) {
                      const match = /language-(\w+)/.exec(className || '');
                      const inline = !match;
                      return !inline && match ? (
                        <SyntaxHighlighter
                          style={vscDarkPlus as any}
                          language={match[1]}
                          PreTag="div"
                          {...props}
                        >
                          {String(children).replace(/\n$/, '')}
                        </SyntaxHighlighter>
                      ) : (
                        <code className={className} {...props}>
                          {children}
                        </code>
                      );
                    },
                  }}
                >
                  {problem.statement}
                </ReactMarkdown>

                {problem.constraints && (
                  <div className="mt-6">
                    <h3>Constraints</h3>
                    <ReactMarkdown>{problem.constraints}</ReactMarkdown>
                  </div>
                )}
              </div>
            </TabsContent>

            <TabsContent value="testcases" className="mt-4 space-y-4">
              {problem.testcases.map((testcase, index) => (
                <div
                  key={testcase.id}
                  className="border rounded-lg p-4 space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <span className="font-semibold">Test Case {index + 1}</span>
                    {testcase.isHidden ? (
                      <Badge variant="secondary" className="gap-1">
                        <EyeOff className="h-3 w-3" />
                        Hidden
                      </Badge>
                    ) : (
                      <Badge variant="outline" className="gap-1">
                        <Eye className="h-3 w-3" />
                        Visible
                      </Badge>
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm font-medium mb-1">Input:</p>
                      <pre className="text-xs bg-muted p-2 rounded overflow-x-auto">
                        {testcase.input}
                      </pre>
                    </div>
                    <div>
                      <p className="text-sm font-medium mb-1">Expected Output:</p>
                      <pre className="text-xs bg-muted p-2 rounded overflow-x-auto">
                        {testcase.expectedOutput}
                      </pre>
                    </div>
                  </div>
                </div>
              ))}
            </TabsContent>

            <TabsContent value="starter" className="mt-4 space-y-4">
              {Object.entries(problem.starterCode).map(([langId, code]) => {
                const langName =
                  langId === '62' ? 'Java' : langId === '71' ? 'Python 3' : langId === '54' ? 'C++' : `Language ${langId}`;
                return (
                  <div key={langId} className="space-y-2">
                    <div className="flex items-center gap-2">
                      <Code className="h-4 w-4" />
                      <span className="font-semibold">{langName}</span>
                    </div>
                    <SyntaxHighlighter
                      language={langId === '62' ? 'java' : langId === '71' ? 'python' : 'cpp'}
                      style={vscDarkPlus}
                      customStyle={{ borderRadius: '0.5rem' }}
                    >
                      {code}
                    </SyntaxHighlighter>
                  </div>
                );
              })}
            </TabsContent>
          </Tabs>
        </ScrollArea>

        <DialogFooter className="border-t pt-4">
          {showRejectInput ? (
            <div className="w-full space-y-3">
              <Textarea
                placeholder="Enter rejection reason (min 10 characters)..."
                value={rejectionReason}
                onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => setRejectionReason(e.target.value)}
                className="min-h-[100px]"
              />
              <div className="flex justify-end gap-2">
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowRejectInput(false);
                    setRejectionReason('');
                  }}
                >
                  Cancel
                </Button>
                <Button
                  variant="destructive"
                  onClick={handleReject}
                  disabled={rejectMutation.isPending || rejectionReason.trim().length < 10}
                >
                  {rejectMutation.isPending ? 'Rejecting...' : 'Confirm Rejection'}
                </Button>
              </div>
            </div>
          ) : (
            <>
              <Button variant="outline" onClick={() => onOpenChange(false)}>
                Close
              </Button>
              <Button
                variant="outline"
                onClick={() => setShowRejectInput(true)}
                className="border-red-200 text-red-600 hover:bg-red-50"
              >
                <X className="h-4 w-4 mr-2" />
                Reject
              </Button>
              <Button
                onClick={handlePublish}
                disabled={publishMutation.isPending}
                className="bg-green-600 hover:bg-green-700"
              >
                <Check className="h-4 w-4 mr-2" />
                {publishMutation.isPending ? 'Publishing...' : 'Publish'}
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
