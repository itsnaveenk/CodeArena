import { Badge } from '@/components/ui/badge';
import { VERDICT_COLORS, type Verdict } from '@/types';
import { cn } from '@/lib/utils';

interface VerdictBadgeProps {
  verdict: Verdict;
  className?: string;
}

const VERDICT_LABELS: Record<Verdict, string> = {
  ACCEPTED: 'Accepted',
  WRONG_ANSWER: 'Wrong Answer',
  TLE: 'Time Limit Exceeded',
  RUNTIME_ERROR: 'Runtime Error',
  COMPILATION_ERROR: 'Compilation Error',
};

export function VerdictBadge({ verdict, className }: VerdictBadgeProps) {
  const colors = VERDICT_COLORS[verdict];

  return (
    <Badge
      variant="secondary"
      className={cn(colors.bg, colors.text, 'font-medium', className)}
    >
      {VERDICT_LABELS[verdict]}
    </Badge>
  );
}
