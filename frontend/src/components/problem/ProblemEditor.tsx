import { useState, useCallback, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { ProblemForm } from './ProblemForm';
import { TestcaseManager } from './TestcaseManager';
import { StarterCodeEditor } from './StarterCodeEditor';
import { ProblemPreview } from './ProblemPreview';
import { StatusBadge } from './StatusBadge';
import { toast } from '@/lib/toast';
import {
  FileText,
  TestTube,
  Code,
  Eye,
  Send,
  Save,
  Loader2,
  AlertCircle,
  CheckCircle,
  Clock,
  XCircle,
  Archive,
} from 'lucide-react';
import type { ProblemFormData } from '@/lib/validations';
import type { ProblemStatus } from '@/types';
import { useAuthStore } from '@/stores/authStore';

interface ProblemEditorProps {
  problemId?: number;
  onSave?: (data: ProblemFormData) => void;
  onCancel?: () => void;
}

type EditorTab = 'details' | 'testcases' | 'starter-code' | 'preview';

const STATUS_WORKFLOW: Record<ProblemStatus, { icon: React.ElementType; color: string; transitions: ProblemStatus[] }> = {
  DRAFT: {
    icon: FileText,
    color: 'text-gray-500',
    transitions: ['PENDING_REVIEW'],
  },
  PENDING_REVIEW: {
    icon: Clock,
    color: 'text-yellow-500',
    transitions: ['PUBLISHED', 'REJECTED'],
  },
  PUBLISHED: {
    icon: CheckCircle,
    color: 'text-green-500',
    transitions: ['ARCHIVED'],
  },
  REJECTED: {
    icon: XCircle,
    color: 'text-red-500',
    transitions: ['PENDING_REVIEW'],
  },
  ARCHIVED: {
    icon: Archive,
    color: 'text-slate-500',
    transitions: [],
  },
};

export function ProblemEditor({ problemId, onSave, onCancel }: ProblemEditorProps) {
  const [activeTab, setActiveTab] = useState<EditorTab>('details');
  const [formData, setFormData] = useState<ProblemFormData | null>(null);
  const [starterCode, setStarterCode] = useState<Record<string, string>>({});
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);
  const queryClient = useQueryClient();

  const { user } = useAuthStore();
  const isAdmin = user?.role === 'ADMIN';

  const isEditMode = !!problemId;

  const { data: problem, isLoading: isLoadingProblem } = useQuery({
    queryKey: queryKeys.problems.detail(problemId!),
    queryFn: () => getApiClient().getProblem(problemId!),
    enabled: isEditMode,
  });

  const { data: testcases } = useQuery({
    queryKey: queryKeys.testcases.byProblem(problemId!),
    queryFn: () => getApiClient().getTestcases(problemId!),
    enabled: isEditMode && problemId! > 0,
  });

  useEffect(() => {
    if (problem) {
      setFormData({
        title: problem.title,
        statement: problem.statement,
        constraints: problem.constraints || '',
        difficulty: problem.difficulty,
        tags: problem.tags,
      });
      setStarterCode(problem.starterCode || {});
    }
  }, [problem]);

  const createMutation = useMutation({
    mutationFn: (data: ProblemFormData) => getApiClient().createProblem({
      ...data,
      starterCode,
    }),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['problems'] });
      queryClient.invalidateQueries({ queryKey: ['myProblems'] });
      toast.success('Problem created successfully');
      setHasUnsavedChanges(false);
      onSave?.(formData!);
      window.location.href = `/edit-problem/${result.id}`;
    },
    onError: (error) => toast.error(error.message),
  });

  const updateMutation = useMutation({
    mutationFn: (data: ProblemFormData) => getApiClient().updateProblem(problemId!, {
      ...data,
      starterCode,
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['problems'] });
      queryClient.invalidateQueries({ queryKey: ['myProblems'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.problems.detail(problemId!) });
      toast.success('Problem updated successfully');
      setHasUnsavedChanges(false);
      onSave?.(formData!);
    },
    onError: (error) => toast.error(error.message),
  });

  const requestReviewMutation = useMutation({
    mutationFn: () => getApiClient().requestReview(problemId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['problems'] });
      queryClient.invalidateQueries({ queryKey: ['myProblems'] });
      queryClient.invalidateQueries({ queryKey: queryKeys.problems.detail(problemId!) });
      toast.success('Problem submitted for review');
    },
    onError: (error) => toast.error(error.message),
  });

  const handleFormChange = useCallback((data: ProblemFormData) => {
    setFormData(data);
    setHasUnsavedChanges(true);
  }, []);

  const handleStarterCodeChange = useCallback((newStarterCode: Record<string, string>) => {
    setStarterCode(newStarterCode);
    setHasUnsavedChanges(true);
  }, []);

  const handleSave = useCallback(() => {
    if (!formData) return;

    if (isEditMode) {
      updateMutation.mutate(formData);
    } else {
      createMutation.mutate(formData);
    }
  }, [formData, isEditMode, updateMutation, createMutation]);

  const handleSubmitForReview = useCallback(() => {
    if (!problemId) return;

    const visibleTestcases = testcases?.filter((tc) => !tc.isHidden) || [];
    if (visibleTestcases.length === 0) {
      toast.error('At least one visible test case is required before submitting for review');
      return;
    }

    requestReviewMutation.mutate();
  }, [problemId, testcases, requestReviewMutation]);

  const canEdit = !problem || canEditProblem(problem.status, isAdmin);
  const canSubmitForReview = isEditMode && problem?.status === 'DRAFT';
  const canResubmit = isEditMode && problem?.status === 'REJECTED';

  const previewData = {
    title: formData?.title || problem?.title || '',
    difficulty: formData?.difficulty || problem?.difficulty || 'EASY',
    tags: formData?.tags || problem?.tags || [],
    statement: formData?.statement || problem?.statement || '',
    constraints: formData?.constraints || problem?.constraints || '',
    examples: testcases || problem?.examples || [],
  };

  const isSaving = createMutation.isPending || updateMutation.isPending;

  if (isEditMode && isLoadingProblem) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-6" data-testid="problem-editor">
      {/* Header with status and actions */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold">
            {isEditMode ? 'Edit Problem' : 'Create Problem'}
          </h1>
          {problem && (
            <StatusBadge status={problem.status} data-testid="problem-status" />
          )}
        </div>
        <div className="flex items-center gap-2">
          {hasUnsavedChanges && (
            <Badge variant="outline" className="text-yellow-600">
              <AlertCircle className="h-3 w-3 mr-1" />
              Unsaved changes
            </Badge>
          )}
          {onCancel && (
            <Button variant="outline" onClick={onCancel}>
              Cancel
            </Button>
          )}
          <Button
            onClick={handleSave}
            disabled={isSaving || !canEdit}
            data-testid="save-button"
          >
            {isSaving ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            Save
          </Button>
          {(canSubmitForReview || canResubmit) && (
            <Button
              onClick={handleSubmitForReview}
              disabled={requestReviewMutation.isPending}
              data-testid="submit-review-button"
            >
              {requestReviewMutation.isPending ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Send className="h-4 w-4 mr-2" />
              )}
              {canResubmit ? 'Resubmit for Review' : 'Submit for Review'}
            </Button>
          )}
        </div>
      </div>

      {/* Status Workflow Indicator */}
      {problem && (
        <StatusWorkflowIndicator
          currentStatus={problem.status}
          data-testid="status-workflow"
        />
      )}

      {/* Rejection reason */}
      {problem?.status === 'REJECTED' && (
        <Card className="border-red-200 bg-red-50 dark:border-red-900 dark:bg-red-950">
          <CardContent className="pt-4">
            <div className="flex items-start gap-2">
              <XCircle className="h-5 w-5 text-red-500 mt-0.5" />
              <div>
                <p className="font-medium text-red-800 dark:text-red-200">
                  Problem Rejected
                </p>
                <p className="text-sm text-red-600 dark:text-red-300">
                  Please review and address the feedback before resubmitting.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Edit restriction notice */}
      {!canEdit && (
        <Card className="border-yellow-200 bg-yellow-50 dark:border-yellow-900 dark:bg-yellow-950">
          <CardContent className="pt-4">
            <div className="flex items-start gap-2">
              <AlertCircle className="h-5 w-5 text-yellow-500 mt-0.5" />
              <div>
                <p className="font-medium text-yellow-800 dark:text-yellow-200">
                  Editing Restricted
                </p>
                <p className="text-sm text-yellow-600 dark:text-yellow-300">
                  {problem?.status === 'PENDING_REVIEW'
                    ? 'This problem is currently under review and cannot be edited.'
                    : 'You do not have permission to edit this problem in its current state.'}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Tabbed Interface */}
      <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as EditorTab)}>
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="details" data-testid="tab-details">
            <FileText className="h-4 w-4 mr-2" />
            <span className="hidden sm:inline">Details</span>
          </TabsTrigger>
          <TabsTrigger
            value="testcases"
            disabled={!isEditMode}
            data-testid="tab-testcases"
          >
            <TestTube className="h-4 w-4 mr-2" />
            <span className="hidden sm:inline">Test Cases</span>
          </TabsTrigger>
          <TabsTrigger value="starter-code" data-testid="tab-starter-code">
            <Code className="h-4 w-4 mr-2" />
            <span className="hidden sm:inline">Starter Code</span>
          </TabsTrigger>
          <TabsTrigger value="preview" data-testid="tab-preview">
            <Eye className="h-4 w-4 mr-2" />
            <span className="hidden sm:inline">Preview</span>
          </TabsTrigger>
        </TabsList>

        {/* Details Tab */}
        <TabsContent value="details" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Problem Details</CardTitle>
            </CardHeader>
            <CardContent>
              <ProblemForm
                initialData={problem}
                onSubmit={handleFormChange}
                isLoading={isSaving}
                autoSave
                disabled={!canEdit}
              />
            </CardContent>
          </Card>
        </TabsContent>

        {/* Test Cases Tab */}
        <TabsContent value="testcases" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Test Cases</CardTitle>
            </CardHeader>
            <CardContent>
              {isEditMode ? (
                <TestcaseManager problemId={problemId!} />
              ) : (
                <p className="text-muted-foreground text-center py-8">
                  Save the problem first to add test cases.
                </p>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Starter Code Tab */}
        <TabsContent value="starter-code" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Starter Code Templates</CardTitle>
            </CardHeader>
            <CardContent>
              <StarterCodeEditor
                value={starterCode}
                onChange={handleStarterCodeChange}
              />
            </CardContent>
          </Card>
        </TabsContent>

        {/* Preview Tab */}
        <TabsContent value="preview" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Problem Preview</CardTitle>
            </CardHeader>
            <CardContent>
              <ProblemPreview {...previewData} />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}

interface StatusWorkflowIndicatorProps {
  currentStatus: ProblemStatus;
  'data-testid'?: string;
}

function StatusWorkflowIndicator({ currentStatus, 'data-testid': testId }: StatusWorkflowIndicatorProps) {
  const statuses: ProblemStatus[] = ['DRAFT', 'PENDING_REVIEW', 'PUBLISHED'];
  const currentIndex = statuses.indexOf(currentStatus);

  return (
    <div className="flex items-center justify-center gap-2 py-4" data-testid={testId}>
      {statuses.map((status, index) => {
        const config = STATUS_WORKFLOW[status];
        const Icon = config.icon;
        const isActive = status === currentStatus;
        const isPast = currentIndex > index;

        return (
          <div key={status} className="flex items-center">
            {index > 0 && (
              <div
                className={`h-0.5 w-8 sm:w-16 mx-2 ${
                  isPast || isActive ? 'bg-primary' : 'bg-muted'
                }`}
              />
            )}
            <div
              className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm ${
                isActive
                  ? 'bg-primary text-primary-foreground'
                  : isPast
                  ? 'bg-primary/20 text-primary'
                  : 'bg-muted text-muted-foreground'
              }`}
              data-testid={`workflow-status-${status.toLowerCase()}`}
            >
              <Icon className="h-4 w-4" />
              <span className="hidden sm:inline">{getStatusLabel(status)}</span>
            </div>
          </div>
        );
      })}

      {/* Show rejected/archived status separately if applicable */}
      {(currentStatus === 'REJECTED' || currentStatus === 'ARCHIVED') && (
        <>
          <div className="h-0.5 w-8 sm:w-16 mx-2 bg-muted" />
          <div
            className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-sm ${
              currentStatus === 'REJECTED'
                ? 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-200'
            }`}
            data-testid={`workflow-status-${currentStatus.toLowerCase()}`}
          >
            {currentStatus === 'REJECTED' ? (
              <XCircle className="h-4 w-4" />
            ) : (
              <Archive className="h-4 w-4" />
            )}
            <span className="hidden sm:inline">{getStatusLabel(currentStatus)}</span>
          </div>
        </>
      )}
    </div>
  );
}

function getStatusLabel(status: ProblemStatus): string {
  const labels: Record<ProblemStatus, string> = {
    DRAFT: 'Draft',
    PENDING_REVIEW: 'In Review',
    PUBLISHED: 'Published',
    REJECTED: 'Rejected',
    ARCHIVED: 'Archived',
  };
  return labels[status];
}

export function getAvailableTransitions(status: ProblemStatus): ProblemStatus[] {
  return STATUS_WORKFLOW[status]?.transitions || [];
}

export function canEditProblem(status: ProblemStatus, isAdmin: boolean = false): boolean {
  if (isAdmin) return true;
  return status === 'DRAFT' || status === 'REJECTED';
}

export default ProblemEditor;
