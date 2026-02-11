import { cn } from '@/lib/utils';
import type { ProblemStatus } from '@/types';

interface StatusBadgeProps {
  status: ProblemStatus;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

const statusConfig: Record<
  ProblemStatus,
  { label: string; className: string }
> = {
  DRAFT: {
    label: 'Draft',
    className: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200',
  },
  PENDING_REVIEW: {
    label: 'Pending Review',
    className: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
  },
  PUBLISHED: {
    label: 'Published',
    className: 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
  },
  REJECTED: {
    label: 'Rejected',
    className: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
  },
  ARCHIVED: {
    label: 'Archived',
    className: 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-200',
  },
};

const sizeClasses = {
  sm: 'text-xs px-2 py-0.5',
  md: 'text-sm px-2.5 py-0.5',
  lg: 'text-base px-3 py-1',
};

export function StatusBadge({ status, size = 'md', className }: StatusBadgeProps) {
  const config = statusConfig[status];

  if (!config) {
    return null;
  }

  return (
    <span
      className={cn(
        'inline-flex items-center font-medium rounded-full',
        config.className,
        sizeClasses[size],
        className
      )}
    >
      {config.label}
    </span>
  );
}

export function getStatusLabel(status: ProblemStatus): string {
  return statusConfig[status]?.label || status;
}

export function getStatusColor(status: ProblemStatus): string {
  return statusConfig[status]?.className || '';
}

export default StatusBadge;
