import { z } from 'zod';

export const emailSchema = z.string()
  .min(1, 'Email is required')
  .email('Invalid email format');

export const passwordSchema = z.string()
  .min(8, 'Password must be at least 8 characters');

export const requiredSchema = z.string()
  .min(1, 'This field is required')
  .refine((val) => val.trim().length > 0, 'This field is required');

export const signupSchema = z.object({
  name: z.string()
    .min(2, 'Name must be at least 2 characters')
    .max(100, 'Name must be at most 100 characters'),
  email: emailSchema,
  password: passwordSchema,
});

export const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Password is required'),
});

export const problemSchema = z.object({
  title: z.string()
    .min(1, 'Title is required')
    .max(255, 'Title must be at most 255 characters'),
  statement: z.string()
    .min(1, 'Problem statement is required'),
  constraints: z.string().optional(),
  difficulty: z.enum(['EASY', 'MEDIUM', 'HARD'] as const),
  tags: z.array(z.string()).optional(),
  starterCode: z.record(z.string(), z.string()).optional(),
});

export const testcaseSchema = z.object({
  input: z.string().min(1, 'Input is required'),
  expectedOutput: z.string().min(1, 'Expected output is required'),
  isHidden: z.boolean(),
});

export const submitCodeSchema = z.object({
  code: z.string().min(1, 'Code cannot be empty'),
  languageId: z.number().min(1, 'Please select a language'),
});
export type SignupFormData = z.infer<typeof signupSchema>;
export type LoginFormData = z.infer<typeof loginSchema>;
export type ProblemFormData = z.infer<typeof problemSchema>;
export type TestcaseFormData = z.infer<typeof testcaseSchema>;
export type SubmitCodeFormData = z.infer<typeof submitCodeSchema>;

export const validateEmail = (email: string): boolean => {
  return emailSchema.safeParse(email).success;
};

export const validatePassword = (password: string): boolean => {
  return passwordSchema.safeParse(password).success;
};

export const validateRequired = (value: string): boolean => {
  return value.trim().length > 0;
};
