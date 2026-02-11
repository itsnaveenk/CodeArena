import { Link } from 'react-router-dom';
import { Calendar, Users, Clock, Trophy } from 'lucide-react';
import type { ContestListItem } from '@/types/contest';
import { ContestStatusBadge } from './ContestStatusBadge';
import { Badge } from '@/components/ui/badge';

interface ContestCardProps {
  contest: ContestListItem;
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function ContestCard({ contest }: ContestCardProps) {
  return (
    <Link
      to={`/contests/${contest.slug}`}
      className="block p-4 border rounded-lg hover:bg-muted/50 transition-colors"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-3 mb-2">
            <h3 className="font-medium truncate">{contest.title}</h3>
            <ContestStatusBadge status={contest.status} />
            {contest.isRegistered && (
              <Badge variant="outline" className="text-xs">Registered</Badge>
            )}
          </div>
          
          <div className="flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
            <div className="flex items-center gap-1">
              <Calendar className="h-4 w-4" />
              <span>{formatDate(contest.startTime)}</span>
            </div>
            
            <div className="flex items-center gap-1">
              <Users className="h-4 w-4" />
              <span>
                {contest.registeredCount}
                {contest.maxParticipants && ` / ${contest.maxParticipants}`}
              </span>
            </div>
            
            <div className="flex items-center gap-1">
              <Clock className="h-4 w-4" />
              <span>{contest.timerMode === 'INDIVIDUAL' ? 'Individual' : 'Global'}</span>
            </div>
            
            <div className="flex items-center gap-1">
              <Trophy className="h-4 w-4" />
              <span>{contest.totalPossiblePoints} pts</span>
            </div>
          </div>
        </div>
        
        <div className="text-right text-sm text-muted-foreground">
          <div>{contest.problemCount} problems</div>
          <div className="text-xs">{contest.category}</div>
        </div>
      </div>
    </Link>
  );
}
