import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { UserStats } from '@/types';

interface DifficultyChartProps {
  stats: UserStats;
}

const COLORS = {
  easy: '#22c55e',
  medium: '#eab308',
  hard: '#ef4444',
};

export function DifficultyChart({ stats }: DifficultyChartProps) {
  const data = [
    { name: 'Easy', value: stats.easySolved, color: COLORS.easy },
    { name: 'Medium', value: stats.mediumSolved, color: COLORS.medium },
    { name: 'Hard', value: stats.hardSolved, color: COLORS.hard },
  ].filter(d => d.value > 0);

  if (data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Problems by Difficulty</CardTitle>
        </CardHeader>
        <CardContent className="h-[200px] flex items-center justify-center">
          <p className="text-muted-foreground">No problems solved yet</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Problems by Difficulty</CardTitle>
      </CardHeader>
      <CardContent className="h-[200px]">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={40}
              outerRadius={70}
              paddingAngle={2}
              dataKey="value"
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={{
                backgroundColor: 'hsl(var(--card))',
                border: '1px solid hsl(var(--border))',
                borderRadius: '8px',
              }}
            />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}
