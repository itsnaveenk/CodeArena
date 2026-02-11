import { Button } from '@/components/ui/button';
import { Loader2 } from 'lucide-react';
import type { ContestDetail, ContestRegistration } from '@/types/contest';
import { useRegisterForContest, useWithdrawFromContest, useStartContest } from '@/hooks/useContests';
import { toast } from 'sonner';

interface ContestRegistrationButtonProps {
  contest: ContestDetail;
  registration?: ContestRegistration | null;
  onSuccess?: () => void;
}

export function ContestRegistrationButton({ contest, registration, onSuccess }: ContestRegistrationButtonProps) {
  const registerMutation = useRegisterForContest();
  const withdrawMutation = useWithdrawFromContest();
  const startMutation = useStartContest();

  const isLoading = registerMutation.isPending || withdrawMutation.isPending || startMutation.isPending;

  const handleRegister = async () => {
    try {
      await registerMutation.mutateAsync(contest.id);
      toast.success('Successfully registered for contest');
      onSuccess?.();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to register');
    }
  };

  const handleWithdraw = async () => {
    try {
      await withdrawMutation.mutateAsync(contest.id);
      toast.success('Successfully withdrawn from contest');
      onSuccess?.();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to withdraw');
    }
  };

  const handleStart = async () => {
    try {
      await startMutation.mutateAsync(contest.id);
      toast.success('Contest started! Good luck!');
      onSuccess?.();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to start contest');
    }
  };

  if (contest.status === 'FINISHED' || contest.status === 'CANCELLED') {
    return null;
  }

  if (!registration || registration.status === 'WITHDRAWN') {
    if (!contest.canRegister) {
      return (
        <Button disabled variant="secondary">
          Registration Closed
        </Button>
      );
    }
    return (
      <Button onClick={handleRegister} disabled={isLoading}>
        {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        Register
      </Button>
    );
  }

  if (contest.status === 'PUBLISHED') {
    return (
      <Button onClick={handleWithdraw} variant="outline" disabled={isLoading}>
        {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        Withdraw
      </Button>
    );
  }

  if (contest.status === 'RUNNING') {
    if (contest.timerMode === 'INDIVIDUAL' && !registration.participantStartTime) {
      if (!contest.canStart) {
        return (
          <Button disabled variant="secondary">
            Cannot Start Yet
          </Button>
        );
      }
      return (
        <Button onClick={handleStart} disabled={isLoading}>
          {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Start Contest
        </Button>
      );
    }

    return (
      <Button variant="default" asChild>
        <a href={`/contests/${contest.slug}/problems`}>Enter Contest</a>
      </Button>
    );
  }

  return null;
}
