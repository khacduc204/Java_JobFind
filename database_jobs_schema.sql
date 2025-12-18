-- Script tạo bảng cho hệ thống tuyển dụng JobFinder

-- Bảng categories (ngành nghề)
CREATE TABLE IF NOT EXISTS categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng jobs (việc làm)
CREATE TABLE IF NOT EXISTS jobs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    employer_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    requirements TEXT,
    location VARCHAR(255),
    salary VARCHAR(100),
    employment_type VARCHAR(50) DEFAULT 'Full-time',
    status VARCHAR(20) NOT NULL DEFAULT 'draft',
    quantity INT DEFAULT NULL,
    deadline DATE DEFAULT NULL,
    view_count INT DEFAULT 0,
    last_viewed_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (employer_id) REFERENCES employers(id) ON DELETE CASCADE,
    INDEX idx_employer (employer_id),
    INDEX idx_status (status),
    INDEX idx_created (created_at),
    INDEX idx_view_count (view_count),
    INDEX idx_deadline (deadline),
    FULLTEXT INDEX idx_fulltext (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng job_categories (nhiều-nhiều giữa jobs và categories)
CREATE TABLE IF NOT EXISTS job_categories (
    job_id INT NOT NULL,
    category_id INT NOT NULL,
    PRIMARY KEY (job_id, category_id),
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE,
    INDEX idx_job (job_id),
    INDEX idx_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng saved_jobs (việc làm đã lưu bởi ứng viên)
CREATE TABLE IF NOT EXISTS saved_jobs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    job_id INT NOT NULL,
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_job (user_id, job_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_job (job_id),
    INDEX idx_saved_at (saved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm dữ liệu mẫu cho categories
INSERT INTO categories (name) VALUES
('Công nghệ thông tin'),
('Kinh doanh / Bán hàng'),
('Marketing'),
('Tài chính / Kế toán'),
('Nhân sự'),
('Hành chính / Văn phòng'),
('Thiết kế đồ họa'),
('Xây dựng'),
('Y tế / Dược phẩm'),
('Giáo dục / Đào tạo'),
('Du lịch / Khách sạn'),
('Vận tải / Logistics'),
('Sản xuất'),
('Luật / Pháp lý'),
('Truyền thông / Media'),
('Điện / Điện tử'),
('Cơ khí / Kỹ thuật'),
('Dịch vụ khách hàng'),
('Thực phẩm / Đồ uống'),
('Thời trang / Làm đẹp')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Thêm dữ liệu mẫu cho jobs (giả sử đã có employer_id = 1)
-- Lưu ý: Chỉ chạy nếu đã có dữ liệu trong bảng employers
INSERT INTO jobs (employer_id, title, description, requirements, location, salary, employment_type, status, quantity, deadline, view_count)
SELECT 1, 'Senior Frontend Developer', 
    '<p>Chúng tôi đang tìm kiếm một Senior Frontend Developer có kinh nghiệm để tham gia đội ngũ phát triển sản phẩm.</p><ul><li>Phát triển giao diện web responsive</li><li>Làm việc với React, Vue.js hoặc Angular</li><li>Tối ưu hiệu suất ứng dụng</li></ul>',
    '<p><strong>Yêu cầu:</strong></p><ul><li>3+ năm kinh nghiệm với JavaScript/TypeScript</li><li>Thành thạo React hoặc Vue.js</li><li>Hiểu biết về REST API, WebSocket</li><li>Kinh nghiệm với Git, CI/CD</li></ul>',
    'Hà Nội', '20 - 30 triệu', 'Full-time', 'published', 2, DATE_ADD(CURDATE(), INTERVAL 30 DAY), 1250
FROM DUAL
WHERE EXISTS (SELECT 1 FROM employers WHERE id = 1)
LIMIT 1;

INSERT INTO jobs (employer_id, title, description, requirements, location, salary, employment_type, status, quantity, deadline, view_count)
SELECT 1, 'Marketing Manager',
    '<p>Vị trí Marketing Manager sẽ chịu trách nhiệm xây dựng và triển khai chiến lược marketing tổng thể cho công ty.</p><ul><li>Lập kế hoạch marketing</li><li>Quản lý ngân sách marketing</li><li>Phân tích hiệu quả chiến dịch</li></ul>',
    '<p><strong>Yêu cầu:</strong></p><ul><li>5+ năm kinh nghiệm trong lĩnh vực Marketing</li><li>Kỹ năng lãnh đạo đội nhóm</li><li>Hiểu biết về Digital Marketing</li><li>Tiếng Anh giao tiếp tốt</li></ul>',
    'TP. Hồ Chí Minh', '25 - 35 triệu', 'Full-time', 'published', 1, DATE_ADD(CURDATE(), INTERVAL 45 DAY), 890
FROM DUAL
WHERE EXISTS (SELECT 1 FROM employers WHERE id = 1)
LIMIT 1;

INSERT INTO jobs (employer_id, title, description, requirements, location, salary, employment_type, status, quantity, deadline, view_count)
SELECT 1, 'Backend Developer (Python)',
    '<p>Tìm kiếm Backend Developer giỏi Python để xây dựng hệ thống backend mạnh mẽ và scalable.</p><ul><li>Phát triển API RESTful</li><li>Tối ưu database queries</li><li>Viết unit tests và documentation</li></ul>',
    '<p><strong>Yêu cầu:</strong></p><ul><li>2+ năm kinh nghiệm Python (Django hoặc FastAPI)</li><li>Kinh nghiệm với PostgreSQL hoặc MySQL</li><li>Hiểu biết về Docker, Kubernetes là lợi thế</li><li>Có tinh thần làm việc nhóm</li></ul>',
    'Hà Nội', '15 - 25 triệu', 'Full-time', 'published', 3, DATE_ADD(CURDATE(), INTERVAL 60 DAY), 650
FROM DUAL
WHERE EXISTS (SELECT 1 FROM employers WHERE id = 1)
LIMIT 1;

-- Gán categories cho jobs (job_id = 1 -> IT, job_id = 2 -> Marketing, job_id = 3 -> IT)
INSERT INTO job_categories (job_id, category_id)
SELECT 1, id FROM categories WHERE name = 'Công nghệ thông tin'
ON DUPLICATE KEY UPDATE job_id = job_id;

INSERT INTO job_categories (job_id, category_id)
SELECT 2, id FROM categories WHERE name = 'Marketing'
ON DUPLICATE KEY UPDATE job_id = job_id;

INSERT INTO job_categories (job_id, category_id)
SELECT 3, id FROM categories WHERE name = 'Công nghệ thông tin'
ON DUPLICATE KEY UPDATE job_id = job_id;

-- Query kiểm tra dữ liệu
SELECT 
    j.id,
    j.title,
    e.company_name,
    j.location,
    j.salary,
    j.view_count,
    j.status,
    GROUP_CONCAT(c.name SEPARATOR ', ') as categories
FROM jobs j
LEFT JOIN employers e ON j.employer_id = e.id
LEFT JOIN job_categories jc ON j.id = jc.job_id
LEFT JOIN categories c ON jc.category_id = c.id
GROUP BY j.id, j.title, e.company_name, j.location, j.salary, j.view_count, j.status;
