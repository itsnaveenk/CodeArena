import { useState } from 'react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import { Loader2, Send, Users } from 'lucide-react';
import { useInviteToContest, useBulkInviteToContest } from '@/hooks/useContests';
import type { BulkInviteResult } from '@/lib/api';
import { toast } from 'sonner';

interface ContestInviteModalProps {
  contestId: number;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onInvited?: () => void;
}

export function ContestInviteModal({ contestId, open, onOpenChange, onInvited }: ContestInviteModalProps) {
  const [mode, setMode] = useState<'single' | 'bulk'>('single');
  const [singleEmail, setSingleEmail] = useState('');
  const [bulkEmails, setBulkEmails] = useState('');
  const [bulkResults, setBulkResults] = useState<BulkInviteResult[]>([]);

  const inviteMutation = useInviteToContest();
  const bulkInviteMutation = useBulkInviteToContest();

  const handleSingleInvite = async () => {
    if (!singleEmail.trim()) return;
    try {
      await inviteMutation.mutateAsync({ contestId, email: singleEmail.trim() });
      toast.success(`Invitation sent to ${singleEmail}`);
      setSingleEmail('');
      onInvited?.();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to send invitation');
    }
  };

  const handleBulkInvite = async () => {
    const emails = bulkEmails
      .split(/[\n,;]+/)
      .map(e => e.trim())
      .filter(e => e.length > 0);

    if (emails.length === 0) return;
    if (emails.length > 100) {
      toast.error('Maximum 100 emails per bulk invite');
      return;
    }

    try {
      const results = await bulkInviteMutation.mutateAsync({ contestId, emails });
      setBulkResults(results);
      onInvited?.();
      const succeeded = results.filter(r => r.success).length;
      if (succeeded > 0) {
        toast.success(`${succeeded} invitation(s) sent`);
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Bulk invite failed');
    }
  };

  const handleClose = (isOpen: boolean) => {
    if (!isOpen) {
      setBulkResults([]);
      setSingleEmail('');
      setBulkEmails('');
    }
    onOpenChange(isOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Invite Participants</DialogTitle>
          <DialogDescription>
            Invite users to this private contest by email.
          </DialogDescription>
        </DialogHeader>

        <div className="flex gap-2 mb-4">
          <Button
            variant={mode === 'single' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setMode('single')}
          >
            <Send className="h-4 w-4 mr-1" />
            Single
          </Button>
          <Button
            variant={mode === 'bulk' ? 'default' : 'outline'}
            size="sm"
            onClick={() => setMode('bulk')}
          >
            <Users className="h-4 w-4 mr-1" />
            Bulk
          </Button>
        </div>

        {mode === 'single' ? (
          <div className="space-y-3">
            <div>
              <Label htmlFor="invite-email">Email address</Label>
              <Input
                id="invite-email"
                type="email"
                placeholder="user@example.com"
                value={singleEmail}
                onChange={(e) => setSingleEmail(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSingleInvite()}
              />
            </div>
          </div>
        ) : (
          <div className="space-y-3">
            <div>
              <Label htmlFor="bulk-emails">
                Email addresses (one per line, comma, or semicolon separated, max 100)
              </Label>
              <Textarea
                id="bulk-emails"
                placeholder={"user1@example.com\nuser2@example.com\nuser3@example.com"}
                value={bulkEmails}
                onChange={(e) => setBulkEmails(e.target.value)}
                rows={6}
              />
            </div>

            {bulkResults.length > 0 && (
              <div className="max-h-48 overflow-y-auto space-y-1">
                {bulkResults.map((result, i) => (
                  <div key={i} className="flex items-center justify-between text-sm py-1">
                    <span className="truncate mr-2">{result.email}</span>
                    <Badge className={result.success ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}>
                      {result.success ? 'invited' : (result.message || 'failed')}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        <DialogFooter>
          <Button variant="outline" onClick={() => handleClose(false)}>
            Cancel
          </Button>
          {mode === 'single' ? (
            <Button
              onClick={handleSingleInvite}
              disabled={inviteMutation.isPending || !singleEmail.trim()}
            >
              {inviteMutation.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              Send Invite
            </Button>
          ) : (
            <Button
              onClick={handleBulkInvite}
              disabled={bulkInviteMutation.isPending || !bulkEmails.trim()}
            >
              {bulkInviteMutation.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              Send All
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
