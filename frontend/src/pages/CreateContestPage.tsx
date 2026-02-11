import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCreateContest } from '@/hooks/useContests';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import type { ContestVisibility, ContestCategory, TimerMode, ScoringModel } from '@/types/contest';
import type { CreateContestRequest } from '@/lib/api';

export default function CreateContestPage() {
  const navigate = useNavigate();
  const createMutation = useCreateContest();
  
  const [formData, setFormData] = useState<CreateContestRequest>({
    title: '',
    description: '',
    visibility: 'PUBLIC',
    category: 'PRACTICE',
    timerMode: 'GLOBAL',
    scoringModel: 'PARTIAL',
    startTime: '',
    endTime: '',
    registrationStartTime: '',
    registrationEndTime: '',
  });

  const handleChange = (field: keyof CreateContestRequest, value: string | number | undefined) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    try {
      const contest = await createMutation.mutateAsync(formData);
      toast.success('Contest created successfully');
      navigate(`/contests/${contest.slug}/manage`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to create contest');
    }
  };

  return (
    <div className="container mx-auto py-8 px-4 max-w-2xl">
      <Card>
        <CardHeader>
          <CardTitle>Create Contest</CardTitle>
          <CardDescription>Set up a new programming contest</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="title">Title</Label>
              <Input
                id="title"
                value={formData.title}
                onChange={(e) => handleChange('title', e.target.value)}
                placeholder="Contest title"
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={formData.description}
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
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="GLOBAL">Global (Same end time for all)</SelectItem>
                    <SelectItem value="INDIVIDUAL">Individual (Personal timer)</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Scoring Model</Label>
                <Select
                  value={formData.scoringModel}
                  onValueChange={(v) => handleChange('scoringModel', v as ScoringModel)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PARTIAL">Partial (Points per test)</SelectItem>
                    <SelectItem value="BINARY">Binary (All or nothing)</SelectItem>
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
                  required
                />
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="startTime">Start Time</Label>
                <Input
                  id="startTime"
                  type="datetime-local"
                  value={formData.startTime}
                  onChange={(e) => handleChange('startTime', e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="endTime">End Time</Label>
                <Input
                  id="endTime"
                  type="datetime-local"
                  value={formData.endTime}
                  onChange={(e) => handleChange('endTime', e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="registrationStartTime">Registration Opens</Label>
                <Input
                  id="registrationStartTime"
                  type="datetime-local"
                  value={formData.registrationStartTime}
                  onChange={(e) => handleChange('registrationStartTime', e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="registrationEndTime">Registration Closes</Label>
                <Input
                  id="registrationEndTime"
                  type="datetime-local"
                  value={formData.registrationEndTime}
                  onChange={(e) => handleChange('registrationEndTime', e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="maxParticipants">Max Participants (optional)</Label>
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
                <Label htmlFor="leaderboardFreezeMinutes">Leaderboard Freeze (minutes)</Label>
                <Input
                  id="leaderboardFreezeMinutes"
                  type="number"
                  min={0}
                  value={formData.leaderboardFreezeMinutes || ''}
                  onChange={(e) => handleChange('leaderboardFreezeMinutes', e.target.value ? parseInt(e.target.value, 10) : undefined)}
                  placeholder="0 (no freeze)"
                />
              </div>
            </div>

            <div className="flex justify-end gap-4">
              <Button type="button" variant="outline" onClick={() => navigate('/contests')}>
                Cancel
              </Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                Create Contest
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
