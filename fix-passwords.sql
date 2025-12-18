-- Fix existing user passwords with BCrypt hash
-- Password for all users: 123456

USE jobfinder;

UPDATE users 
SET password_hash = '$2a$10$8E1PJoKxmcG7YnzcrfIK8.djFvwnoRvLyKLudx7SFWwdJLlw8sEKC'
WHERE email IN (
    'admin@gmail.com',
    'ndd@gmail.com',
    'bang@email.com',
    'company1@jobfinder.com',
    'company2@jobfinder.com'
);

-- Verify the update
SELECT id, email, LEFT(password_hash, 10) as hash_start, LENGTH(password_hash) as hash_length
FROM users 
WHERE email IN ('admin@gmail.com', 'ndd@gmail.com', 'bang@email.com')
ORDER BY id;
