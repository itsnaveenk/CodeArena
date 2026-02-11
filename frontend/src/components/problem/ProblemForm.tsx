import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { problemSchema, type ProblemFormData } from '@/lib/validations';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Loader2 } from 'lucide-react';
import type { Difficulty, ProblemDetail } from '@/types';

interface ProblemFormProps {
  initialData?: ProblemDetail;
  onSubmit: (data: ProblemFormData) => void;
  isLoading?: boolean;
  disabled?: boolean;
}

const DIFFICULTIES: Difficulty[] = ['EASY', 'MEDIUM', 'HARD'];

export function ProblemForm({ initialData, onSubmit, isLoading, autoSave, disabled }: ProblemFormProps) {
  const lastValuesRef = useRef<string>('');
  
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    getValues,
    formState: { errors },
  } = useForm<ProblemFormData>({
    resolver: zodResolver(problemSchema),
    defaultValues: {
      title: initialData?.title || '',
      statement: initialData?.statement || '',
      constraints: initialData?.constraints || '',
      difficulty: initialData?.difficulty || 'EASY',
      tags: initialData?.tags || [],
    },
  });

  const difficulty = watch('difficulty');
  const allValues = watch();

  useEffect(() => {
    if (autoSave) {
      const values = getValues();
      const valuesStr = JSON.stringify(values);
      
      if (valuesStr !== lastValuesRef.current) {
        lastValuesRef.current = valuesStr;
        onSubmit(values);
      }
    }
  }, [allValues, autoSave, getValues, onSubmit]);

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="title">Title</Label>
        <Input
          id="title"
          placeholder="Two Sum"
          {...register('title')}
          aria-invalid={!!errors.title}
          disabled={disabled}
        />
        {errors.title && (
          <p className="text-sm text-destructive">{errors.title.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="difficulty">Difficulty</Label>
        <Select
          value={difficulty}
          onValueChange={(v) => setValue('difficulty', v as Difficulty)}
          disabled={disabled}
        >
          <SelectTrigger>
            <SelectValue placeholder="Select difficulty" />
          </SelectTrigger>
          <SelectContent>
            {DIFFICULTIES.map((d) => (
              <SelectItem key={d} value={d}>
                {d.charAt(0) + d.slice(1).toLowerCase()}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        {errors.difficulty && (
          <p className="text-sm text-destructive">{errors.difficulty.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="tags">Tags (comma-separated)</Label>
        <Input
          id="tags"
          placeholder="array, hash-table, two-pointers"
          disabled={disabled}
          defaultValue={initialData?.tags?.join(', ') || ''}
          {...register('tags', {
            setValueAs: (v: string | string[]) => {
              if (Array.isArray(v)) return v;
              return v ? v.split(',').map(t => t.trim()).filter(Boolean) : [];
            },
          })}
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="statement">Problem Statement</Label>
        <textarea
          id="statement"
          className="w-full min-h-[200px] p-3 border rounded-md bg-background resize-y disabled:opacity-50 disabled:cursor-not-allowed"
          placeholder="Given an array of integers nums and an integer target..."
          disabled={disabled}
          {...register('statement')}
          aria-invalid={!!errors.statement}
        />
        {errors.statement && (
          <p className="text-sm text-destructive">{errors.statement.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="constraints">Constraints</Label>
        <textarea
          id="constraints"
          className="w-full min-h-[100px] p-3 border rounded-md bg-background resize-y font-mono text-sm disabled:opacity-50 disabled:cursor-not-allowed"
          placeholder="2 <= nums.length <= 10^4&#10;-10^9 <= nums[i] <= 10^9"
          disabled={disabled}
          {...register('constraints')}
        />
      </div>

      {!autoSave && (
        <Button type="submit" disabled={isLoading || disabled}>
          {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          {initialData ? 'Update Problem' : 'Create Problem'}
        </Button>
      )}
    </form>
  );
}
