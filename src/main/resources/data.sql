-- Initialize admin user after schema creation
-- This will run after Hibernate creates the schema

-- Update first user to ADMIN role
UPDATE users SET role = 'ADMIN' WHERE id = 1;

-- Update user with email setter@example.com to PROBLEM_SETTER
UPDATE users SET role = 'PROBLEM_SETTER' WHERE email = 'setter@example.com';

-- Update user with email admin@example.com to ADMIN
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@example.com';
