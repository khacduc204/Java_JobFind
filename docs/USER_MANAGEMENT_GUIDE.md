# Quản lý Người Dùng - Hướng Dẫn Sử Dụng

## Tính năng đã hoàn thành ✅

### 1. Danh sách người dùng (`/admin/users`)
- Hiển thị toàn bộ người dùng trong hệ thống
- Các cột: ID, Ảnh đại diện, Email, Tên, Vai trò
- Nút thao tác: Sửa, Xóa (có xác nhận modal)
- Flash messages cho các thông báo thành công/lỗi

### 2. Thêm người dùng mới (`/admin/users/add`)
- Form nhập thông tin: Tên, Email, Mật khẩu, Vai trò
- Upload ảnh đại diện (tùy chọn)
- Preview ảnh trước khi upload
- Validation:
  - Email phải unique
  - Mật khẩu tối thiểu 8 ký tự
  - Tất cả trường bắt buộc phải có giá trị

### 3. Chỉnh sửa người dùng (`/admin/users/edit/{id}`)
- Form sửa thông tin: Tên, Vai trò
- Email không thể thay đổi (readonly)
- Upload ảnh đại diện mới (tùy chọn)
- Preview ảnh hiện tại
- Hiển thị thông tin: ID, Ngày tạo, Số điện thoại

### 4. Xóa người dùng (`POST /admin/users/delete/{id}`)
- Xác nhận xóa qua modal Bootstrap
- Tự động xóa ảnh đại diện khi xóa user
- Flash message thông báo kết quả

## Cấu trúc File

```
src/main/java/com/example/JobFinder/
└── controller/
    └── UserAdminController.java          # Controller xử lý CRUD

src/main/resources/
├── templates/
│   └── admin/
│       └── users/
│           ├── users.html                # Danh sách user
│           ├── add-user.html             # Form thêm mới
│           └── edit-user.html            # Form chỉnh sửa
└── static/
    └── uploads/
        └── avatars/                      # Thư mục lưu avatar
```

## Endpoints API

| Method | URL | Mô tả |
|--------|-----|-------|
| GET | `/admin/users` | Danh sách tất cả người dùng |
| GET | `/admin/users/add` | Form thêm người dùng |
| POST | `/admin/users/add` | Xử lý thêm người dùng |
| GET | `/admin/users/edit/{id}` | Form sửa người dùng |
| POST | `/admin/users/edit/{id}` | Xử lý cập nhật người dùng |
| POST | `/admin/users/delete/{id}` | Xóa người dùng |

## Security

- Chỉ user có ROLE_ADMIN mới truy cập được
- Đã cấu hình trong SecurityConfig: `.requestMatchers("/admin/**").hasRole("ADMIN")`
- Mật khẩu được hash bằng BCrypt trước khi lưu database

## Upload Avatar

### Quy tắc
- Định dạng hỗ trợ: PNG, JPG, GIF, WEBP
- Kích thước tối đa: 2MB (có thể config thêm)
- Tên file: UUID random + extension
- Thư mục lưu: `src/main/resources/static/uploads/avatars/`
- URL truy cập: `/uploads/avatars/{filename}`

### Xử lý
- Khi upload ảnh mới: Xóa ảnh cũ (nếu có)
- Khi xóa user: Xóa avatar tự động
- Preview real-time khi chọn file

## Giao diện

### Thiết kế
- Sử dụng NiceAdmin template (Bootstrap 5)
- Responsive design
- Icons: Bootstrap Icons
- Flash messages: Bootstrap alerts với auto-dismiss

### Màu sắc
- Primary (Xanh dương): Nút "Sửa", "Lưu"
- Success (Xanh lá): Nút "Thêm người dùng"
- Danger (Đỏ): Nút "Xóa"
- Secondary (Xám): Nút "Hủy", "Quay lại"

## Test

### Dữ liệu test hiện có
- admin@gmail.com / 123456 (ROLE_ADMIN)
- ndd@gmail.com / 123456
- bang@email.com / 123456

### Test flow
1. Đăng nhập với admin@gmail.com
2. Truy cập `/admin/users`
3. Thêm user mới với đầy đủ thông tin + avatar
4. Kiểm tra user xuất hiện trong danh sách
5. Sửa thông tin user
6. Thử đổi avatar
7. Xóa user (xác nhận modal)

## Lưu ý kỹ thuật

### Avatar Upload Path
- Development: `src/main/resources/static/uploads/avatars/`
- Production: Nên chuyển sang external storage (AWS S3, Cloudinary, etc.)

### Database
- Cột `avatar_path` trong bảng `users`: VARCHAR(255)
- Lưu path relative: `/uploads/avatars/{filename}`

### Lazy Loading Issue
- Đã fix lỗi "Could not initialize proxy" bằng:
  - `@Transactional(readOnly = true)` trong CustomUserDetailsService
  - JOIN FETCH trong UserRepository query

### Flash Messages
- Sử dụng RedirectAttributes.addFlashAttribute()
- Tự động hiển thị và dismiss sau redirect
- 3 loại: successMessage, warningMessage, errorMessage

## TODO (Cải tiến tương lai)

- [ ] Phân trang cho danh sách user (khi > 50 users)
- [ ] Tìm kiếm và lọc theo vai trò
- [ ] Export danh sách user ra Excel/CSV
- [ ] Bulk actions (xóa nhiều user cùng lúc)
- [ ] Resize và optimize avatar tự động
- [ ] Email notification khi tạo user mới
- [ ] User activity log (audit trail)
- [ ] Soft delete thay vì hard delete
