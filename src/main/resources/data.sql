-- Seed roles (khớp repo PHP gốc)
INSERT IGNORE INTO roles (id, name, description)
VALUES
	(1, 'admin', 'Administrator with full access'),
	(2, 'employer', 'Employer account'),
	(3, 'candidate', 'Candidate account');

-- Seed permissions theo FindJob
INSERT IGNORE INTO permissions (name, description)
VALUES
	('manage_users', 'Create/update/delete users'),
	('manage_jobs', 'Create/update/delete job posts'),
	('apply_jobs', 'Apply to jobs'),
	('view_applications', 'View job applications');

-- Gán permissions cho từng role giống PHP
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
WHERE (r.name = 'admin')
	OR (r.name = 'employer' AND p.name IN ('manage_jobs', 'view_applications'))
	OR (r.name = 'candidate' AND p.name IN ('apply_jobs'));

-- Seed một vài danh mục mặc định để trang lọc việc làm không trống
INSERT IGNORE INTO job_categories (name, description) VALUES
('Công nghệ thông tin', 'Lập trình, kiểm thử, hệ thống'),
('Marketing', 'Quảng cáo, SEO, sáng tạo nội dung'),
('Kinh doanh', 'Bán hàng, tư vấn khách hàng');

