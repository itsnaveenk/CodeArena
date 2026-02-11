import { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useContestBySlug, useUpdateContest } from '@/hooks/useContests';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import type { ContestVisibility, ContestCategory, TimerMode, ScoringModel } from '@/types/contest';
import type { UpdateContestRequest } from '@/lib/api';

export default function EditContestPage() {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { data: contest, isLoading } = useContestBySlug(slug || '');
  const updateMutation = useUpdateContest();
  
  const [formData, setFormData] = useState<UpdateContestRequest>({});

  useEffect(() => {
    if (contest) {
      setFormData({
        title: contest.title,
        description: contest.description,
        visibility: contest.visibility,
        category: contest.category,
        timerMode: contest.timerMode,
        scoringModel: contest.scoringModel,
        startTime: contest.startTime.slice(0, 16),
        endTime: contest.endTime.slice(0, 16),
        durationMinutes: contest.durationMinutes,
        registrationStartTime: contest.registrationStartTime.slice(0, 16),
        registrationEndTime: contest.registrationEndTime.slice(0, 16),
        maxParticipants: contest.maxParticipants,
        leaderboardFreezeMinutes: contest.leaderboardFreezeMinutes,
      });
    }
  }, [contest]);

  const handleChange = (field: keyof UpdateContestRequest, value: string | number | undefined) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!contest) return;
    
    try {
      await updateMutation.mutateAsync({ id: contest.id, data: formData });
      toast.success('Contest updated successfully');
      navigate(`/contests/${slug}/manage`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update contest');
    }
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-8 px-4 max-w-2xl">
        <Skeleton className="h-8 w-1/4 mb-4" />
        <Skeleton className="h-96 w-full" />
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

  const isEditable = contest.status === 'DRAFT' || contest.status === 'PUBLISHED';

  if (!isEditable) {
    return (
      <div className="container mx-auto py-8 px-4 text-center">
        <p className="text-muted-foreground mb-4">
          This contest cannot be edited because it is {contest.status.toLowerCase()}.
        </p>
        <Button asChild>
          <Link to={`/contests/${slug}`}>Back to Contest</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8 px-4 max-w-2xl">
      <div className="flex items-center gap-4 mb-6">
        <Button asChild variant="ghost" size="sm">
          <Link to={`/contests/${slug}/manage`}>
            <ArrowLeft className="h-4 w-4 mr-1" />
            Back
          </Link>
        </Button>
        <h1 className="text-2xl font-bold">Edit Contest</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>{contest.title}</CardTitle>
          <CardDescription>
            {contest.status === 'PUBLISHED' && 'Some fields may be restricted for published contests.'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="title">Title</Label>
              <Input
                id="title"
                value={formData.title || ''}
                onChange={(e) => handleChange('title', e.target.value)}
                placeholder="Contest title"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={formData.description || ''}
                onChange={(e) => handleChange('description', e.target.value)}
                placeholder="Contest description and rules"
                rows={4}
              />
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Visibility</Label>
                <Select
                  value={formData.visibility}
                  onValueChange={(v) => handleChange('visibility', v as ContestVisibility)}
                  disabled={contest.status !== 'DRAFT'}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PUBLIC">Public</SelectItem>
                    <SelectItem value="PRIVATE">Private (Invite Only)</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Category</Label>
                <Select
                  value={formData.category}
                  onValueChange={(v) => handleChange('category', v as ContestCategory)}
                  disabled={contest.status !== 'DRAFT'}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PRACTICE">Practice</SelectItem>
                    <SelectItem value="RATED">Rated</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Timer Mode</Label>
                <Select
                  value={formData.timerMode}
                  onValueChange={(v) => handleChange('timerMode', v as TimerMode)}
                  disabled={contest.status !== 'DRAFT'}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="GLOBAL">Global</SelectItem>
                    <SelectItem value="INDIVIDUAL">Individual</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Scoring Model</Label>
                <Select
                  value={formData.scoringModel}
                  onValueChange={(v) => handleChange('scoringModel', v as ScoringModel)}
                  disabled={contest.status !== 'DRAFT'}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PARTIAL">Partial</SelectItem>
                    <SelectItem value="BINARY">Binary</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {formData.timerMode === 'INDIVIDUAL' && (
              <div className="space-y-2">
                <Label htmlFor="durationMinutes">Duration (minutes)</Label>
                <Input
                  id="durationMinutes"
                  type="number"
                  min={1}
                  value={formData.durationMinutes || ''}
                  onChange={(e) => handleChange('durationMinutes', parseInt(e.target.value, 10))}
                  placeholder="e.g., 120"
                  disabled={contest.status !== 'DRAFT'}
                />
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="startTime">Start Time</Label>
                <Input
                  id="startTime"
                  type="datetime-local"
                  value={formData.startTime || ''}
                  onChange={(e) => handleChange('startTime', e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="endTime">End Time</Label>
                <Input
                  id="endTime"
                  type="datetime-local"
                  value={formData.endTime || ''}
                  onChange={(e) => handleChange('endTime', e.target.value)}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="registrationStartTime">Registration Opens</Label>
                <Input
                  id="registrationStartTime"
                  type="datetime-local"
                  value={formData.registrationStartTime || ''}
                  onChange={(e) => handleChange('registrationStartTime', e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="registrationEndTime">Registration Closes</Label>
                <Input
                  id="registrationEndTime"
                  type="datetime-local"
                  value={formData.registrationEndTime || ''}
                  onChange={(e) => handleChange('registrationEndTime', e.target.value)}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="maxParticipants">Max Participants</Label>
                <Input
                  id="maxParticipants"
                  type="number"
                  min={1}
                  value={formData.maxParticipants || ''}
                  onChange={(e) => handleChange('maxParticipants', e.target.value ? parseInt(e.target.value, 10) : undefined)}
                  placeholder="Unlimited"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="leaderboardFreezeMinutes">Leaderboard Freeze (min)</Label>
                <Input
                  id="leaderboardFreezeMinutes"
                  type="number"
                  min={0}
                  value={formData.leaderboardFreezeMinutes || ''}
                  onChange={(e) => handleChange('leaderboardFreezeMinutes', e.target.value ? parseInt(e.target.value, 10) : undefined)}
                  placeholder="0"
                />
              </div>
            </div>

            <div className="flex justify-end gap-4">
              <Button type="button" variant="outline" onClick={() => navigate(`/contests/${slug}/manage`)}>
                Cancel
              </Button>
              <Button type="submit" disabled={updateMutation.isPending}>
                {updateMutation.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                Save Changes
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
