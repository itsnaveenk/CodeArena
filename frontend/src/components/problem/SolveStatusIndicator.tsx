import { CheckCircle, Circle } from 'lucide-react';
import { SOLVE_STATUS_ICONS, type SolveStatus } from '@/types';
import { cn } from '@/lib/utils';

interface SolveStatusIndicatorProps {
  status: SolveStatus;
  className?: string;
}

export function SolveStatusIndicator({ status, className }: SolveStatusIndicatorProps) {
  const config = SOLVE_STATUS_ICONS[status];

  if (status === 'SOLVED') {
    return <CheckCircle className={cn('h-5 w-5', config.color, className)} />;
  }

  if (status === 'ATTEMPTED') {
    return <Circle className={cn('h-5 w-5', config.color, className)} />;
  }

  return <Circle className={cn('h-5 w-5', config.color, className)} />;
}
