import { useState, useEffect, useCallback, useRef } from 'react';

interface UseContestTimerOptions {
  remainingSeconds?: number;
  onTimeUp?: () => void;
  remaining: number;
  isLowTime: boolean;
  isExpired: boolean;
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

export function useContestTimer({
  endTime,
  remainingSeconds,
  onTimeUp,
  enabled = true,
}: UseContestTimerOptions): UseContestTimerReturn {
  const onTimeUpRef = useRef(onTimeUp);
  onTimeUpRef.current = onTimeUp;

  const hasCalledTimeUp = useRef(false);

  const calculateRemaining = useCallback(() => {
    if (!enabled) return 0;
    if (remainingSeconds !== undefined) return Math.max(0, remainingSeconds);
    if (endTime) {
      const end = new Date(endTime).getTime();
      return Math.max(0, Math.floor((end - Date.now()) / 1000));
    }
    return 0;
  }, [endTime, remainingSeconds, enabled]);

  const [remaining, setRemaining] = useState(calculateRemaining);

  useEffect(() => {
    setRemaining(calculateRemaining());
    hasCalledTimeUp.current = false;
  }, [calculateRemaining]);

  useEffect(() => {
    if (!enabled || remaining <= 0) {
      if (remaining <= 0 && !hasCalledTimeUp.current && onTimeUpRef.current) {
        hasCalledTimeUp.current = true;
        onTimeUpRef.current();
      }
      return;
    }

    const timer = setInterval(() => {
      setRemaining(prev => {
        const next = prev - 1;
        if (next <= 0 && !hasCalledTimeUp.current && onTimeUpRef.current) {
          hasCalledTimeUp.current = true;
          onTimeUpRef.current();
        }
        return Math.max(0, next);
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [remaining, enabled]);

  return {
    remaining,
    formatted: formatTime(remaining),
    isLowTime: remaining > 0 && remaining <= 300,
    isCriticalTime: remaining > 0 && remaining <= 60,
    isExpired: remaining <= 0,
  };
}
