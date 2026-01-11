# JobFinder – Luồng nghiệp vụ & cách vận hành

## Tổng quan
JobFinder là ứng dụng tuyển dụng Spring Boot 3.5 (Java 21+/25) với mô hình monolith. Phân tầng controller → service → repository dưới `src/main/java/com/example/JobFinder`, giao diện Thymeleaf dưới `src/main/resources/templates`, asset tĩnh tại `src/main/resources/static`. CSDL MySQL/MariaDB; cấu hình trong `src/main/resources/application.properties`.

## Kiến trúc & bảo mật
- Spring Security: điều hướng theo role qua `SecurityConfig` và `RoleBasedAuthenticationSuccessHandler`; public: `/jobs/**`, `/auth/**`, assets; private: `/admin/**`, `/employer/**`, `/candidate/**`.
- Entity chính: `User`, `Employer`, `Candidate`, `Job`, `Category`, `Application`, `SavedJob`, `Notification` (tham chiếu trong `model/`).
- Repository dùng JPA/JPQL tránh N+1 (ví dụ `JobRepository`, `ApplicationRepository`).
- View: frontend ở `templates/frontend/**`, employer ở `templates/employer/**`, admin ở `templates/admin/**`, layout chia `templates/layout/**` và `templates/fragments/**`.

## Luồng nghiệp vụ chính
### Đăng nhập & phân quyền
1) Người dùng đăng nhập tại `/auth/login` (xem `AuthController`).
2) Sau khi thành công, handler chuyển hướng theo role: EMPLOYER → `/employer/dashboard`, CANDIDATE → `/candidate/dashboard`, ADMIN → `/admin/dashboard`.

### Nhà tuyển dụng (Employer)
- Dashboard: thống kê job, ứng tuyển, thông báo (`EmployerController#employerDashboard`).
- Quản lý job: `/employer/jobs` (list), `/employer/jobs/create` (tạo), `/employer/jobs/edit/{id}` (sửa), `/employer/jobs/delete/{id}` (xóa). Form dùng TinyMCE cho mô tả/yêu cầu; lưu qua `JobRepository`. Categories được nạp từ `CategoryRepository`.
- Quản lý ứng viên: `/employer/applications` lọc theo job/trạng thái; xem chi tiết `/employer/applications/{id}`; thay đổi trạng thái kèm email/thông báo (`NotificationService`, `EmailService`).
- Hồ sơ doanh nghiệp: cập nhật logo, thông tin công ty (upload lưu `static/uploads/logos`).

### Ứng viên (Candidate)
- Dashboard: đề xuất job, việc đã lưu, trạng thái ứng tuyển.
- Duyệt job: `/jobs`, `/jobs/{id}`; lọc theo keyword, địa điểm, loại hình, danh mục (`JobController`, `JobService`).
- Lưu job: toggle `/jobs/{id}/save` lưu vào `SavedJob`.
- Ứng tuyển: gửi cover letter và CV (upload `uploads/cv`), lưu `Application` liên kết `Candidate` + `Job` (`ApplicationController`).
- Hồ sơ cá nhân: cập nhật thông tin, ảnh đại diện (upload `static/uploads/avatars`).

### Admin
- Quản trị người dùng/role/permission, danh mục, việc làm, đơn ứng tuyển theo hướng dẫn `docs/USER_MANAGEMENT_GUIDE.md`. Giao diện ở `templates/admin/**`.

### Thông báo & email
- Khi ứng tuyển hoặc trạng thái thay đổi, tạo bản ghi `Notification` và gửi email qua `EmailService` (SMTP cấu hình trong properties). Thông báo được hiển thị trên dashboard employer/candidate.

## Dòng chảy dữ liệu job
1) Employer tạo/sửa job → `Job` lưu vào bảng `jobs`, map danh mục qua `job_category_map`.
2) Job được hiển thị public nếu `status='published'` và `deadline` còn hạn.
3) Candidate xem chi tiết, lưu, hoặc ứng tuyển. `Application` ghi nhận trạng thái, đính kèm CV/cover letter.
4) Employer theo dõi ứng tuyển, thay đổi trạng thái, ứng viên nhận thông báo/email.

## Hướng dẫn chi tiết
### Đăng bài tuyển dụng (Employer)
1) Vào `/employer/jobs/create` (hoặc nút “Đăng tin” trong danh sách job).
2) Nhập **Tiêu đề**, **Mô tả** (TinyMCE), tùy chọn **Yêu cầu**, chọn **Danh mục** (ít nhất một), điền **Địa điểm**, **Mức lương**, **Hình thức làm việc**, **Số lượng**, **Hạn nộp** (bỏ trống nếu không giới hạn), chọn **Trạng thái**:
	- `draft`: lưu nháp, không public.
	- `published`: hiển thị ở trang `/jobs` cho ứng viên.
3) Bấm **Đăng tin/Cập nhật**. Thành công sẽ nhận flash “Đã tạo/cập nhật tin tuyển dụng thành công” và quay về `/employer/jobs`.
4) Chỉnh sửa hoặc đóng tin: `/employer/jobs/edit/{id}` đổi `status` thành `closed` để ẩn khỏi public.

### Ứng tuyển (Candidate)
1) Duyệt job ở `/jobs` hoặc xem chi tiết `/jobs/{id}`.
2) Nhấn **Ứng tuyển**: nhập cover letter (mô tả ngắn) và đính kèm CV (kiểm tra dung lượng/định dạng theo `ApplicationController.handleCvUpload`).
3) Gửi thành công sẽ lưu `Application` (trạng thái `applied`), hiển thị trong dashboard ứng viên và trang `/employer/applications` của nhà tuyển dụng.
4) Theo dõi trạng thái: employer có thể đổi `viewed/shortlisted/rejected/hired/withdrawn`, ứng viên nhận thông báo và (nếu bật) email.

## Thiết lập & chạy
### Yêu cầu
- JDK 21+ (code khai báo Java 25 tương thích).
- MySQL/MariaDB, database `jobfinder`.

### Khởi tạo DB
1) Tạo database `jobfinder`.
2) Import thứ tự: `schema.sql` → `database_jobs_schema.sql` (dữ liệu mẫu jobs/categories) → tùy chọn `data.sql` (seed người dùng/role). Đường dẫn: `schema.sql`, `database_jobs_schema.sql`, `src/main/resources/data.sql`.
3) Kiểm tra/điều chỉnh `spring.datasource.*` trong `src/main/resources/application.properties`.

### Chạy ứng dụng
- Windows: `mvnw.cmd spring-boot:run`
- Hoặc build: `mvnw.cmd clean package -DskipTests` rồi chạy jar `target/JobFinder-0.0.1-SNAPSHOT.jar`.
- Truy cập: `http://localhost:8080`.

## Thư mục quan trọng
- Controller: [src/main/java/com/example/JobFinder/controller](src/main/java/com/example/JobFinder/controller)
- Service: [src/main/java/com/example/JobFinder/service](src/main/java/com/example/JobFinder/service)
- Repository: [src/main/java/com/example/JobFinder/repository](src/main/java/com/example/JobFinder/repository)
- Entity: [src/main/java/com/example/JobFinder/model](src/main/java/com/example/JobFinder/model)
- Templates: [src/main/resources/templates](src/main/resources/templates)
- Static assets: [src/main/resources/static](src/main/resources/static)
- Cấu hình: [src/main/resources/application.properties](src/main/resources/application.properties)

## Kiểm thử nhanh
- Truy cập `/jobs` (public) để xem danh sách job.
- Đăng nhập tài khoản employer mẫu (từ `data.sql` nếu seed) → tạo job mới → kiểm tra hiển thị ở `/jobs` khi status `published`.
- Đăng nhập candidate mẫu → lưu job và ứng tuyển, kiểm tra job đã lưu và ứng tuyển trên dashboard.

## Sự cố thường gặp
- 500 khi render job do lệch cột DB ↔ entity: so khớp bảng `jobs` với entity `Job` (cột `job_requirements`, `quantity`, `deadline`, `view_count`…).
- Upload lỗi do quyền thư mục `static/uploads/**` hoặc vượt kích thước: xem kiểm tra file trong `ApplicationController.handleCvUpload()` và logo upload ở `EmployerController`.
- Không đăng nhập được: kiểm tra kết nối DB và dữ liệu seed (roles/users) trong `data.sql`.

## Mở rộng
- Nếu thêm route public, nhớ cập nhật permit trong `SecurityConfig`.
- Khi thêm thuộc tính job/application, đồng bộ cả entity, repository, template và DDL seed.
