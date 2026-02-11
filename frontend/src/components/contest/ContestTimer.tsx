import { useState, useEffect, useCallback } from 'react';
import { Clock } from 'lucide-react';
import { cn } from '@/lib/utils';

interface ContestTimerProps {
  endTime?: string;
  remainingSeconds?: number;
  onTimeUp?: () => void;
  className?: string;
}

function formatTime(totalSeconds: number): string {
  if (totalSeconds <= 0) return '00:00:00';
  
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  
  return [hours, minutes, seconds]
    .map(v => v.toString().padStart(2, '0'))
    .join(':');
}

export function ContestTimer({ endTime, remainingSeconds, onTimeUp, className }: ContestTimerProps) {
  const calculateRemaining = useCallback(() => {
    if (remainingSeconds !== undefined) {
      return remainingSeconds;
    }
    if (endTime) {
      const end = new Date(endTime).getTime();
      const now = Date.now();
      return Math.max(0, Math.floor((end - now) / 1000));
    }
    return 0;
  }, [endTime, remainingSeconds]);

  const [remaining, setRemaining] = useState(calculateRemaining);
  const [hasCalledTimeUp, setHasCalledTimeUp] = useState(false);

  useEffect(() => {
    setRemaining(calculateRemaining());
    setHasCalledTimeUp(false);
  }, [calculateRemaining]);

  useEffect(() => {
    if (remaining <= 0) {
      if (!hasCalledTimeUp && onTimeUp) {
        setHasCalledTimeUp(true);
        onTimeUp();
      }
      return;
    }

    const timer = setInterval(() => {
      setRemaining(prev => {
        const next = prev - 1;
        if (next <= 0 && !hasCalledTimeUp && onTimeUp) {
          setHasCalledTimeUp(true);
          onTimeUp();
        }
        return Math.max(0, next);
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [remaining, onTimeUp, hasCalledTimeUp]);

  const isLowTime = remaining > 0 && remaining <= 300; // 5 minutes
  const isCriticalTime = remaining > 0 && remaining <= 60; // 1 minute

  return (
    <div
      className={cn(
        'flex items-center gap-2 font-mono text-lg',
        isLowTime && 'text-yellow-600 dark:text-yellow-400',
        isCriticalTime && 'text-red-600 dark:text-red-400 animate-pulse',
        className
      )}
    >
      <Clock className="h-5 w-5" />
      <span>{formatTime(remaining)}</span>
    </div>
  );
}
