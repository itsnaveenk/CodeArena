import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';

import { AlertCircle } from 'lucide-react';

interface RejectionReasonAlertProps {
  reason: string;
  rejectedAt?: string;
  rejectedByName?: string;
}

export function RejectionReasonAlert({ reason, rejectedAt, rejectedByName }: RejectionReasonAlertProps) {
  return (
    <Alert variant="destructive" className="mb-6">
      <AlertCircle className="h-4 w-4" />
      <AlertTitle>Problem Rejected</AlertTitle>
      <AlertDescription className="mt-2 space-y-2">
        <p className="font-medium">Rejection Reason:</p>
        <p className="text-sm whitespace-pre-wrap">{reason}</p>
        {(rejectedAt || rejectedByName) && (
          <p className="text-xs text-muted-foreground mt-2">
            {rejectedByName && `Rejected by ${rejectedByName}`}
            {rejectedAt && rejectedByName && ' on '}
            {rejectedAt && new Date(rejectedAt).toLocaleString()}
          </p>
        )}
        <p className="text-sm mt-3">
          Please address the feedback above and resubmit your problem for review.
        </p>
      </AlertDescription>
    </Alert>
  );
}
