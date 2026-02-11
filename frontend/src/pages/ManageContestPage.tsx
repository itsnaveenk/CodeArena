import { useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import {
  useContestBySlug,
  useContestProblems,
  useContestInvitations,
  usePublishContest,
  useCancelContest,
  useDeleteContest,
  useAddContestProblem,
  useRemoveContestProblem,
  useRevokeContestInvitation,
} from '@/hooks/useContests';
import { ContestStatusBadge, ContestInviteModal } from '@/components/contest';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ArrowLeft, Edit, Trash2, Plus, Send, X, Loader2, AlertTriangle, CheckCircle } from 'lucide-react';
import { toast } from 'sonner';
import { getApiClient } from '@/lib/api';

export default function ManageContestPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  
  const { data: contest, isLoading, refetch } = useContestBySlug(slug || '');
  const { data: problems, refetch: refetchProblems } = useContestProblems(contest?.id || 0);
  const { data: invitations, refetch: refetchInvitations } = useContestInvitations(contest?.id || 0);
  
  const publishMutation = usePublishContest();
  const cancelMutation = useCancelContest();
  const deleteMutation = useDeleteContest();
  const addProblemMutation = useAddContestProblem();
  const removeProblemMutation = useRemoveContestProblem();
  const revokeInvitationMutation = useRevokeContestInvitation();
  
  const [newProblemId, setNewProblemId] = useState('');
  const [newProblemPoints, setNewProblemPoints] = useState('100');
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [validationResult, setValidationResult] = useState<{ valid: boolean; errors: string[] } | null>(null);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);
  const [showCancelDialog, setShowCancelDialog] = useState(false);

  const handleValidate = async () => {
    if (!contest) return;
    try {
      const result = await getApiClient().validateContest(contest.id);
      setValidationResult(result);
    } catch (error) {
      toast.error('Failed to validate contest');
    }
  };

  const handlePublish = async () => {
    if (!contest) return;
    try {
      await publishMutation.mutateAsync(contest.id);
      toast.success('Contest published successfully');
      refetch();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to publish');
    }
  };

  const handleCancel = async () => {
    if (!contest) return;
    try {
      await cancelMutation.mutateAsync(contest.id);
      toast.success('Contest cancelled');
      refetch();
      setShowCancelDialog(false);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to cancel');
    }
  };

  const handleDelete = async () => {
    if (!contest) return;
    try {
      await deleteMutation.mutateAsync(contest.id);
      toast.success('Contest deleted');
      navigate('/contests');
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to delete');
    }
  };

  const handleAddProblem = async () => {
    if (!contest || !newProblemId) return;
    try {
      await addProblemMutation.mutateAsync({
        contestId: contest.id,
        data: {
          problemId: parseInt(newProblemId, 10),
          pointValue: parseInt(newProblemPoints, 10) || 100,
        },
      });
      toast.success('Problem added');
      setNewProblemId('');
      setNewProblemPoints('100');
      refetchProblems();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to add problem');
    }
  };

  const handleRemoveProblem = async (problemId: number) => {
    if (!contest) return;
    try {
      await removeProblemMutation.mutateAsync({ contestId: contest.id, problemId });
      toast.success('Problem removed');
      refetchProblems();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to remove problem');
    }
  };

  const handleRevokeInvitation = async (invitationId: number) => {
    if (!contest) return;
    try {
      await revokeInvitationMutation.mutateAsync({ contestId: contest.id, invitationId });
      toast.success('Invitation revoked');
      refetchInvitations();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to revoke invitation');
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-8 px-4">
        <Skeleton className="h-8 w-1/4 mb-4" />
        <Skeleton className="h-64 w-full mb-4" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (!contest) {
    return (
      <div className="container mx-auto py-8 px-4 text-center">
        <p className="text-destructive mb-4">Contest not found</p>
        <Button asChild>
          <Link to="/contests">Back to Contests</Link>
        </Button>
      </div>
    );
  }

  const canModify = contest.status === 'DRAFT' || contest.status === 'PUBLISHED';
  const canPublish = contest.status === 'DRAFT';
  const canCancel = contest.status === 'PUBLISHED' || contest.status === 'RUNNING';
  const canDelete = contest.status === 'DRAFT';

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <Button asChild variant="ghost" size="sm">
            <Link to={`/contests/${slug}`}>
              <ArrowLeft className="h-4 w-4 mr-1" />
              Back
            </Link>
          </Button>
          <h1 className="text-2xl font-bold">{contest.title}</h1>
          <ContestStatusBadge status={contest.status} />
        </div>
        
        <div className="flex gap-2">
          {canModify && (
            <Button asChild variant="outline" size="sm">
              <Link to={`/contests/${slug}/edit`}>
                <Edit className="h-4 w-4 mr-1" />
                Edit
              </Link>
            </Button>
          )}
          {canDelete && (
            <Button variant="destructive" size="sm" onClick={() => setShowDeleteDialog(true)}>
              <Trash2 className="h-4 w-4 mr-1" />
              Delete
            </Button>
          )}
        </div>
      </div>

      {/* Status Actions */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Contest Status</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center gap-4">
            {canPublish && (
              <>
                <Button variant="outline" onClick={handleValidate}>
                  Validate
                </Button>
                <Button onClick={handlePublish} disabled={publishMutation.isPending}>
                  {publishMutation.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                  Publish Contest
                </Button>
              </>
            )}
            {canCancel && (
              <Button variant="destructive" onClick={() => setShowCancelDialog(true)}>
                Cancel Contest
              </Button>
            )}
          </div>
          
          {validationResult && (
            <div className={`mt-4 p-4 rounded-lg ${validationResult.valid ? 'bg-green-100 dark:bg-green-900/30' : 'bg-red-100 dark:bg-red-900/30'}`}>
              <div className="flex items-center gap-2 mb-2">
                {validationResult.valid ? (
                  <CheckCircle className="h-5 w-5 text-green-600" />
                ) : (
                  <AlertTriangle className="h-5 w-5 text-red-600" />
                )}
                <span className="font-medium">
                  {validationResult.valid ? 'Contest is ready to publish' : 'Validation failed'}
                </span>
              </div>
              {validationResult.errors.length > 0 && (
                <ul className="list-disc list-inside text-sm">
                  {validationResult.errors.map((error, i) => (
                    <li key={i}>{error}</li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Problems */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle>Problems</CardTitle>
          <CardDescription>{problems?.length || 0} problems added</CardDescription>
        </CardHeader>
        <CardContent>
          {canModify && (
            <div className="flex gap-2 mb-4">
              <div className="flex-1">
                <Label htmlFor="problemId" className="sr-only">Problem ID</Label>
                <Input
                  id="problemId"
                  type="number"
                  placeholder="Problem ID"
                  value={newProblemId}
                  onChange={(e) => setNewProblemId(e.target.value)}
                />
              </div>
              <div className="w-32">
                <Label htmlFor="points" className="sr-only">Points</Label>
                <Input
                  id="points"
                  type="number"
                  placeholder="Points"
                  value={newProblemPoints}
                  onChange={(e) => setNewProblemPoints(e.target.value)}
                />
              </div>
              <Button onClick={handleAddProblem} disabled={addProblemMutation.isPending}>
                <Plus className="h-4 w-4 mr-1" />
                Add
              </Button>
            </div>
          )}
          
          {problems && problems.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>#</TableHead>
                  <TableHead>Title</TableHead>
                  <TableHead>Points</TableHead>
                  {canModify && <TableHead className="w-16"></TableHead>}
                </TableRow>
              </TableHeader>
              <TableBody>
                {problems.map((problem, index) => (
                  <TableRow key={problem.id}>
                    <TableCell>{String.fromCharCode(65 + index)}</TableCell>
                    <TableCell>{problem.title}</TableCell>
                    <TableCell>{problem.pointValue}</TableCell>
                    {canModify && (
                      <TableCell>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleRemoveProblem(problem.problemId)}
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    )}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <p className="text-muted-foreground text-center py-4">No problems added yet.</p>
          )}
        </CardContent>
      </Card>

      {/* Invitations (for PRIVATE contests) */}
      {contest.visibility === 'PRIVATE' && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>Invitations</CardTitle>
                <CardDescription>Manage invitations for this private contest</CardDescription>
              </div>
              {canModify && (
                <Button size="sm" onClick={() => setShowInviteModal(true)}>
                  <Send className="h-4 w-4 mr-1" />
                  Invite
                </Button>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {invitations && invitations.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Email</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Invited At</TableHead>
                    {canModify && <TableHead className="w-16"></TableHead>}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {invitations.map((invitation) => (
                    <TableRow key={invitation.id}>
                      <TableCell>{invitation.invitedEmail}</TableCell>
                      <TableCell>
                        <Badge variant="secondary">{invitation.status}</Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(invitation.invitedAt).toLocaleDateString()}
                      </TableCell>
                      {canModify && (
                        <TableCell>
                          {invitation.status === 'PENDING' && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleRevokeInvitation(invitation.id)}
                            >
                              <X className="h-4 w-4" />
                            </Button>
                          )}
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <p className="text-muted-foreground text-center py-4">No invitations sent yet.</p>
            )}
          </CardContent>
        </Card>
      )}

      {contest.visibility === 'PRIVATE' && (
        <ContestInviteModal
          contestId={contest.id}
          open={showInviteModal}
          onOpenChange={setShowInviteModal}
          onInvited={() => refetchInvitations()}
        />
      )}

      <ConfirmDialog
        open={showDeleteDialog}
        onOpenChange={setShowDeleteDialog}
        title="Delete Contest"
        description="Are you sure you want to delete this contest? This action cannot be undone."
        confirmLabel="Delete"
        onConfirm={handleDelete}
        variant="destructive"
      />

      <ConfirmDialog
        open={showCancelDialog}
        onOpenChange={setShowCancelDialog}
        title="Cancel Contest"
        description="Are you sure you want to cancel this contest? Participants will be notified."
        confirmLabel="Cancel Contest"
        onConfirm={handleCancel}
        variant="destructive"
      />
    </div>
  );
}
