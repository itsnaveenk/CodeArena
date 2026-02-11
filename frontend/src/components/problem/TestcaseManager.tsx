import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { getApiClient } from '@/lib/api';
import { queryKeys } from '@/lib/queryClient';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { toast } from '@/lib/toast';
import { Plus, Trash2, Eye, EyeOff, Loader2, Pencil } from 'lucide-react';
import type { CreateTestcaseRequest, Testcase } from '@/types';

interface TestcaseManagerProps {
  problemId: number;
}

type DialogMode = 'add' | 'edit';

export function TestcaseManager({ problemId }: TestcaseManagerProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<DialogMode>('add');
  const [editingTestcaseId, setEditingTestcaseId] = useState<number | null>(null);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);
  const [formData, setFormData] = useState<CreateTestcaseRequest>({
    input: '',
    expectedOutput: '',
    isHidden: false,
  });
  const queryClient = useQueryClient();

  const { data: testcases, isLoading } = useQuery({
    queryKey: queryKeys.testcases.byProblem(problemId),
    queryFn: () => getApiClient().getTestcases(problemId),
    enabled: problemId > 0,
  });

  const addMutation = useMutation({
    mutationFn: (data: CreateTestcaseRequest) => getApiClient().addTestcase(problemId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.testcases.byProblem(problemId) });
      toast.success('Testcase added');
      closeDialog();
    },
    onError: (error) => toast.error(error.message),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: CreateTestcaseRequest }) =>
      getApiClient().updateTestcase(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.testcases.byProblem(problemId) });
      toast.success('Testcase updated');
      closeDialog();
    },
    onError: (error) => toast.error(error.message),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => getApiClient().deleteTestcase(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.testcases.byProblem(problemId) });
      toast.success('Testcase deleted');
      setDeleteConfirmId(null);
    },
    onError: (error) => toast.error(error.message),
  });

  const closeDialog = () => {
    setIsDialogOpen(false);
    setDialogMode('add');
    setEditingTestcaseId(null);
    setFormData({ input: '', expectedOutput: '', isHidden: false });
  };

  const openAddDialog = () => {
    setDialogMode('add');
    setFormData({ input: '', expectedOutput: '', isHidden: false });
    setIsDialogOpen(true);
  };

  const openEditDialog = (testcase: Testcase) => {
    setDialogMode('edit');
    setEditingTestcaseId(testcase.id);
    setFormData({
      input: testcase.input,
      expectedOutput: testcase.expectedOutput,
      isHidden: testcase.isHidden,
    });
    setIsDialogOpen(true);
  };

  const handleSubmit = () => {
    if (!formData.input.trim() || !formData.expectedOutput.trim()) {
      toast.error('Input and expected output are required');
      return;
    }
    
    if (dialogMode === 'edit' && editingTestcaseId) {
      updateMutation.mutate({ id: editingTestcaseId, data: formData });
    } else {
      addMutation.mutate(formData);
    }
  };

  const isSubmitting = addMutation.isPending || updateMutation.isPending;

  if (isLoading) {
    return (
      <div className="space-y-2">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-12 w-full" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h3 className="font-semibold">Test Cases ({testcases?.length || 0})</h3>
        <Button size="sm" onClick={openAddDialog}>
          <Plus className="h-4 w-4 mr-1" />
          Add Testcase
        </Button>
      </div>

      {testcases?.length === 0 ? (
        <p className="text-muted-foreground text-center py-4">
          No test cases yet. Add at least one test case.
        </p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>#</TableHead>
              <TableHead>Input (preview)</TableHead>
              <TableHead>Expected Output (preview)</TableHead>
              <TableHead>Visibility</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {testcases?.map((tc, index) => (
              <TableRow key={tc.id}>
                <TableCell>{index + 1}</TableCell>
                <TableCell className="font-mono text-xs max-w-[200px] truncate">
                  {tc.input.slice(0, 50)}{tc.input.length > 50 ? '...' : ''}
                </TableCell>
                <TableCell className="font-mono text-xs max-w-[200px] truncate">
                  {tc.expectedOutput.slice(0, 50)}{tc.expectedOutput.length > 50 ? '...' : ''}
                </TableCell>
                <TableCell>
                  {tc.isHidden ? (
                    <Badge variant="secondary" data-testid="hidden-indicator">
                      <EyeOff className="h-3 w-3 mr-1" />
                      Hidden
                    </Badge>
                  ) : (
                    <Badge variant="outline">
                      <Eye className="h-3 w-3 mr-1" />
                      Visible
                    </Badge>
                  )}
                </TableCell>
                <TableCell>
                  <div className="flex gap-1">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => openEditDialog(tc)}
                      data-testid={`edit-testcase-${tc.id}`}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setDeleteConfirmId(tc.id)}
                      data-testid={`delete-testcase-${tc.id}`}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Dialog open={isDialogOpen} onOpenChange={(open) => !open && closeDialog()}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              {dialogMode === 'edit' ? 'Edit Test Case' : 'Add Test Case'}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="input">Input</Label>
              <textarea
                id="input"
                className="w-full min-h-[100px] p-3 border rounded-md bg-background font-mono text-sm resize-y"
                placeholder="Enter test input..."
                value={formData.input}
                onChange={(e) => setFormData({ ...formData, input: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="expectedOutput">Expected Output</Label>
              <textarea
                id="expectedOutput"
                className="w-full min-h-[100px] p-3 border rounded-md bg-background font-mono text-sm resize-y"
                placeholder="Enter expected output..."
                value={formData.expectedOutput}
                onChange={(e) => setFormData({ ...formData, expectedOutput: e.target.value })}
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="isHidden"
                checked={formData.isHidden}
                onChange={(e) => setFormData({ ...formData, isHidden: e.target.checked })}
              />
              <Label htmlFor="isHidden">Hidden (not shown to users)</Label>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={closeDialog}>
              Cancel
            </Button>
            <Button onClick={handleSubmit} disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              {dialogMode === 'edit' ? 'Save Changes' : 'Add Testcase'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        title="Delete Test Case"
        description="Are you sure you want to delete this test case? This action cannot be undone."
        confirmLabel="Delete"
        variant="destructive"
        open={deleteConfirmId !== null}
        onOpenChange={(open) => !open && setDeleteConfirmId(null)}
        onConfirm={() => deleteConfirmId && deleteMutation.mutate(deleteConfirmId)}
        onCancel={() => setDeleteConfirmId(null)}
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
