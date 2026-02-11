import { useState, useEffect } from 'react';
import type { Difficulty, ProblemFilter } from '@/types';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Button } from '@/components/ui/button';
import { Search, X } from 'lucide-react';

interface ProblemFiltersProps {
  filter: ProblemFilter;
  onFilterChange: (filter: ProblemFilter) => void;
}

const DIFFICULTIES: { value: Difficulty | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'All Difficulties' },
  { value: 'EASY', label: 'Easy' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HARD', label: 'Hard' },
];

export function ProblemFilters({ filter, onFilterChange }: ProblemFiltersProps) {
  const [searchInput, setSearchInput] = useState(filter.search || '');

  useEffect(() => {
    const timer = setTimeout(() => {
      if (searchInput !== filter.search) {
        onFilterChange({ ...filter, search: searchInput || undefined, page: 0 });
      }
    }, 300);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const handleDifficultyChange = (value: string) => {
    const difficulty = value === 'ALL' ? undefined : (value as Difficulty);
    onFilterChange({ ...filter, difficulty, page: 0 });
  };

  const handleClearFilters = () => {
    setSearchInput('');
    onFilterChange({ page: 0, size: filter.size });
  };

  const hasActiveFilters = filter.difficulty || filter.search;

  return (
    <div className="flex flex-col sm:flex-row gap-3 mb-6">
      <div className="relative flex-1">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
        <Input
          placeholder="Search problems..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          className="pl-9"
        />
      </div>
      <Select
        value={filter.difficulty || 'ALL'}
        onValueChange={handleDifficultyChange}
      >
        <SelectTrigger className="w-full sm:w-[180px]">
          <SelectValue placeholder="Difficulty" />
        </SelectTrigger>
        <SelectContent>
          {DIFFICULTIES.map((d) => (
            <SelectItem key={d.value} value={d.value}>
              {d.label}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      {hasActiveFilters && (
        <Button variant="ghost" size="icon" onClick={handleClearFilters}>
          <X className="h-4 w-4" />
        </Button>
      )}
    </div>
  );
}
