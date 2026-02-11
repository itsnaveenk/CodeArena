import { Badge } from '@/components/ui/badge';
import { DIFFICULTY_COLORS, type Difficulty } from '@/types';
import { cn } from '@/lib/utils';

interface DifficultyBadgeProps {
  difficulty: Difficulty;
  className?: string;
}

export function DifficultyBadge({ difficulty, className }: DifficultyBadgeProps) {
  const colors = DIFFICULTY_COLORS[difficulty];

  return (
    <Badge
      variant="secondary"
      className={cn(colors.bg, colors.text, 'font-medium', className)}
    >
      {difficulty}
    </Badge>
  );
}
