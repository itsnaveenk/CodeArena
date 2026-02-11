import { Badge } from '@/components/ui/badge';
import type { ContestStatus } from '@/types/contest';
import { CONTEST_STATUS_COLORS } from '@/types/contest';

interface ContestStatusBadgeProps {
  status: ContestStatus;
  className?: string;
}

const STATUS_LABELS: Record<ContestStatus, string> = {
  DRAFT: 'Draft',
  PUBLISHED: 'Upcoming',
  RUNNING: 'Live',
  FINISHED: 'Finished',
  CANCELLED: 'Cancelled',
};

export function ContestStatusBadge({ status, className }: ContestStatusBadgeProps) {
  const colors = CONTEST_STATUS_COLORS[status];
  
  return (
    <Badge 
      variant="secondary" 
      className={`${colors.bg} ${colors.text} ${className || ''}`}
    >
      {status === 'RUNNING' && (
        <span className="mr-1 h-2 w-2 rounded-full bg-green-500 animate-pulse" />
      )}
      {STATUS_LABELS[status]}
    </Badge>
  );
}
