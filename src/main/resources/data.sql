-- Seed roles
INSERT IGNORE INTO roles (id, name, description)
VALUES
	(1, 'admin', 'Quản trị hệ thống'),
	(2, 'employer', 'Nhà tuyển dụng'),
	(3, 'candidate', 'Ứng viên tìm việc');

-- Seed permissions (tối giản cho Sprint 1)
INSERT IGNORE INTO permissions (id, name, description)
VALUES
	(1, 'USER_READ', 'Xem thông tin tài khoản'),
	(2, 'USER_WRITE', 'Cập nhật tài khoản');

-- Map permissions to roles
INSERT IGNORE INTO role_permissions (role_id, permission_id)
VALUES
	(1, 1), (1, 2),
	(2, 1);

