import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { StatusBadge } from '@/components/problem/StatusBadge';
import { DifficultyBadge } from '@/components/problem/DifficultyBadge';
import type { ProblemManagementItem } from '@/lib/api';

type BulkAction = 'publish' | 'reject' | 'archive';

interface BulkActionDialogProps {
  action: BulkAction | null;
  selectedProblems: ProblemManagementItem[];
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  isLoading?: boolean;
}

const ACTION_CONFIG: Record<BulkAction, { title: string; description: string; variant: 'default' | 'destructive' }> = {
  publish: {
    title: 'Publish Problems',
    description: 'This will make the selected problems visible to all users.',
    variant: 'default',
  },
  reject: {
    title: 'Reject Problems',
    description: 'This will reject the selected problems and notify their authors.',
    variant: 'destructive',
  },
  archive: {
    title: 'Archive Problems',
    description: 'This will archive the selected problems and hide them from users.',
    variant: 'default',
  },
};

export function BulkActionDialog({
  action,
  selectedProblems,
  open,
  onOpenChange,
  onConfirm,
  isLoading = false,
}: BulkActionDialogProps) {
  if (!action) {
    return null;
  }

  const config = ACTION_CONFIG[action];

  return (
    <ConfirmDialog
      open={open}
      onOpenChange={onOpenChange}
      title={`${config.title} (${selectedProblems.length})`}
      description={
        <div className="space-y-4">
          <p>{config.description}</p>
          <p className="text-sm text-muted-foreground">
            The following problems will be affected:
          </p>
          <div className="max-h-60 overflow-y-auto border rounded-md">
            <table className="w-full text-sm">
              <thead className="bg-muted sticky top-0">
                <tr>
                  <th className="text-left p-2">Title</th>
                  <th className="text-left p-2">Status</th>
                  <th className="text-left p-2">Difficulty</th>
                </tr>
              </thead>
              <tbody>
                {selectedProblems.map((problem) => (
                  <tr key={problem.id} className="border-t">
                    <td className="p-2">
                      <div className="font-medium">{problem.title}</div>
                      <div className="text-xs text-muted-foreground">
                        by {problem.authorName}
                      </div>
                    </td>
                    <td className="p-2">
                      <StatusBadge status={problem.status} size="sm" />
                    </td>
                    <td className="p-2">
                      <DifficultyBadge difficulty={problem.difficulty} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      }
      confirmLabel={`${action.charAt(0).toUpperCase()}${action.slice(1)} ${selectedProblems.length} Problem${selectedProblems.length > 1 ? 's' : ''}`}
      onConfirm={onConfirm}
      isLoading={isLoading}
      variant={config.variant}
    />
  );
}

export default BulkActionDialog;
