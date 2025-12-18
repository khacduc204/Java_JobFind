-- Script to rehash existing passwords to BCrypt
-- Run this ONLY if you know the plain text passwords

-- Example: Update known passwords
-- BCrypt hash for "123456" (common test password)
UPDATE users SET password_hash = '$2a$10$6M7rEfEJ4VzQYxlC3xZxFeVY8FXLYo6.EYm8vvC9qF0gq8K5Y0sFy'
WHERE email = 'admin@gmail.com';

UPDATE users SET password_hash = '$2a$10$6M7rEfEJ4VzQYxlC3xZxFeVY8FXLYo6.EYm8vvC9qF0gq8K5Y0sFy'
WHERE email = 'ndd@gmail.com';

UPDATE users SET password_hash = '$2a$10$6M7rEfEJ4VzQYxlC3xZxFeVY8FXLYo6.EYm8vvC9qF0gq8K5Y0sFy'
WHERE email = 'bang@email.com';

-- If you need to generate BCrypt hashes for other passwords, use this Java code:
-- System.out.println(new BCryptPasswordEncoder().encode("your_password"));
