import type { MedalType } from '@/types/contest';
import { MEDAL_ICONS } from '@/types/contest';

interface MedalBadgeProps {
  medal: MedalType;
  className?: string;
}

export function MedalBadge({ medal, className }: MedalBadgeProps) {
  return (
    <span className={`text-lg ${className || ''}`} title={`${medal} Medal`}>
      {MEDAL_ICONS[medal]}
    </span>
  );
}
