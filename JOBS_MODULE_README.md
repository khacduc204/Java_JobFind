# Hệ Thống Tuyển Dụng JobFinder - Module Jobs

## 🎉 Đã hoàn thành

Tôi đã tạo đầy đủ hệ thống quản lý và hiển thị tin tuyển dụng giống repo PHP của bạn, bao gồm:

### ✅ Backend (Spring Boot)

#### 1. **Entities (Model)**
- ✅ `Job.java` - Entity cho việc làm
- ✅ `Category.java` - Entity cho ngành nghề
- ✅ `SavedJob.java` - Entity cho việc làm đã lưu
- Các quan hệ: Many-to-Many (Job ↔ Category), Many-to-One (Job → Employer)

#### 2. **Repositories**
- ✅ `JobRepository.java` - Query phức tạp với JPQL
  - Tìm kiếm với nhiều bộ lọc (keyword, location, employment type, category)
  - Hot jobs (xếp hạng theo view count)
  - Featured jobs (mới nhất)
  - Related jobs (cùng ngành nghề)
- ✅ `CategoryRepository.java` - Quản lý ngành nghề
- ✅ `SavedJobRepository.java` - Quản lý việc làm đã lưu

#### 3. **Services**
- ✅ `JobService.java` - Logic nghiệp vụ đầy đủ
  - Tìm kiếm & filter việc làm
  - Lấy hot jobs theo khoảng thời gian
  - Quản lý saved jobs
  - Format time ago (giống PHP repo)
  - Convert Job entity sang Map cho template

#### 4. **Controllers**
- ✅ `JobController.java`
  - `GET /jobs` - Danh sách việc làm với filter
  - `GET /jobs/{id}` - Chi tiết việc làm
  - `GET /jobs/hot` - Việc làm hot
  - `GET /jobs?saved=true` - Việc làm đã lưu
  - `POST /jobs/save` - Lưu/bỏ lưu việc (AJAX)

### ✅ Frontend (Thymeleaf Templates)

#### 1. **Trang danh sách việc làm** (`jobs/index.html`)
```
/jobs
/jobs?saved=true
```
**Tính năng:**
- ✅ Filter sidebar (keyword, location, category, employment type, sort)
- ✅ Job cards với logo, title, company, meta info
- ✅ View count, badges, categories
- ✅ Save button (heart icon) với AJAX
- ✅ Pagination đầy đủ
- ✅ Empty state (chưa có jobs hoặc chưa lưu)
- ✅ Responsive design

#### 2. **Trang chi tiết việc làm** (`jobs/detail.html`)
```
/jobs/{id}
```
**Tính năng:**
- ✅ Job header với company logo, title, meta grid
- ✅ Job description & requirements (hỗ trợ HTML)
- ✅ Company about section
- ✅ Sidebar: thông tin chung, apply button, save button
- ✅ Deadline warning
- ✅ Social share buttons (Facebook, Twitter, Copy link)
- ✅ Related jobs section (placeholder)
- ✅ View count increment tự động

#### 3. **Trang việc làm hot** (`jobs/hot.html`)
```
/jobs/hot?range=30
```
**Tính năng:**
- ✅ Hot ranking badges (gold #1, #2, #3...)
- ✅ View count badges với animation
- ✅ HOT badges với fire icon
- ✅ Time range filter (7, 30, 90 ngày)
- ✅ 2 columns layout
- ✅ Pagination
- ✅ Save functionality

### ✅ Security & Configuration
- ✅ Cập nhật `SecurityConfig.java`: cho phép truy cập `/jobs/**` không cần login
- ✅ Public access cho trang danh sách và chi tiết
- ✅ Save jobs chỉ cho CANDIDATE role

### ✅ Database Schema
File: `database_jobs_schema.sql`

**Bảng đã tạo:**
1. **categories** - Ngành nghề (20 categories mẫu)
2. **jobs** - Việc làm (với view_count, last_viewed_at)
3. **job_categories** - Quan hệ nhiều-nhiều
4. **saved_jobs** - Việc làm đã lưu

**Sample data:**
- 20 ngành nghề (IT, Marketing, Tài chính, Y tế...)
- 3 việc làm mẫu với đầy đủ thông tin

---

## 🚀 Hướng dẫn chạy

### Bước 1: Tạo database
```sql
-- Import file SQL
source d:/TTCN_DA/JobFinder/database_jobs_schema.sql;

-- Hoặc copy/paste vào phpMyAdmin
```

### Bước 2: Build project
```bash
cd d:\TTCN_DA\JobFinder
.\mvnw.cmd clean package -DskipTests
```

### Bước 3: Chạy application
```bash
.\mvnw.cmd spring-boot:run
```

### Bước 4: Truy cập
```
http://localhost:8080/jobs           # Danh sách việc làm
http://localhost:8080/jobs/1         # Chi tiết việc làm #1
http://localhost:8080/jobs/hot       # Việc làm hot
http://localhost:8080/jobs?saved=true # Việc làm đã lưu (cần login)
```

---

## 📁 Cấu trúc files đã tạo

```
JobFinder/
├── src/main/java/com/example/JobFinder/
│   ├── model/
│   │   ├── Job.java                    ✅ NEW
│   │   ├── Category.java               ✅ NEW
│   │   └── SavedJob.java               ✅ NEW
│   │
│   ├── repository/
│   │   ├── JobRepository.java          ✅ NEW
│   │   ├── CategoryRepository.java     ✅ NEW
│   │   └── SavedJobRepository.java     ✅ NEW
│   │
│   ├── service/
│   │   └── JobService.java             ✅ NEW
│   │
│   ├── controller/
│   │   └── JobController.java          ✅ NEW
│   │
│   └── config/
│       └── SecurityConfig.java         ✅ UPDATED
│
├── src/main/resources/templates/frontend/jobs/
│   ├── index.html                      ✅ NEW - Danh sách jobs
│   ├── detail.html                     ✅ NEW - Chi tiết job
│   └── hot.html                        ✅ NEW - Hot jobs
│
└── database_jobs_schema.sql            ✅ NEW
```

---

## 🎨 Thiết kế giống PHP repo

### 1. **Trang danh sách** giống `public/job/share/index.php`
- Filter sidebar giống y hệt
- Job cards với layout giống
- View count, save button, categories
- Pagination logic tương tự

### 2. **Trang hot jobs** giống `public/job/share/hot.php`
- Ranking badges (#1, #2, #3...)
- View count prominent display
- Time range filter (7/30/90 ngày)
- Hot badges với animation

### 3. **Trang chi tiết** giống `public/job/share/view.php`
- Company logo & header
- Meta grid layout
- Description & requirements sections
- Sidebar với thông tin chung
- Apply & save buttons

---

## 🔧 Các tính năng chính

### Search & Filter
```java
// Controller: /jobs?keyword=java&location=hanoi&categoryId=1
jobService.getPublishedJobsWithFilters(keyword, location, employmentType, categoryId, sort, page, perPage)
```

### Hot Jobs
```java
// Controller: /jobs/hot?range=30
jobService.getHotJobs(withinDays, page, perPage)
// Sắp xếp theo view_count DESC, last_viewed_at DESC
```

### Saved Jobs
```java
// Controller: /jobs?saved=true (cần CANDIDATE role)
jobService.getSavedJobsByUser(userId, page, perPage)

// AJAX: POST /jobs/save?jobId=1
jobService.toggleSaveJob(userId, jobId)
```

### View Count
```java
// Tự động tăng khi vào trang chi tiết
jobService.getJobDetail(jobId, incrementView=true)
```

---

## 💡 Lưu ý quan trọng

### 1. User Authentication
Controller hiện tại có placeholder cho `currentUserId`:
```java
// TODO: Get actual user ID from UserService
Integer currentUserId = null;
```

**Cần implement:**
```java
// Trong JobController, thêm UserService
@Autowired
private UserService userService;

// Trong method
User currentUser = userService.getCurrentUser(authentication);
Integer currentUserId = currentUser.getId();
```

### 2. Related Jobs
Hiện tại trả về list rỗng. Cần implement:
```java
// Trong JobService
public List<Map<String, Object>> getRelatedJobs(Integer jobId, int limit) {
    Job job = jobRepository.findById(jobId).orElse(null);
    if (job == null) return Collections.emptyList();
    
    List<Integer> categoryIds = job.getCategories().stream()
        .map(Category::getId)
        .collect(Collectors.toList());
    
    List<Job> relatedJobs = jobRepository.findRelatedJobs(
        job.getEmployer().getId(), 
        categoryIds, 
        PageRequest.of(0, limit)
    );
    
    return relatedJobs.stream()
        .map(this::jobToMap)
        .collect(Collectors.toList());
}
```

### 3. Apply Job
Chức năng "Ứng tuyển ngay" chưa implement. Cần tạo:
- `Application.java` entity
- `ApplicationRepository.java`
- `ApplicationService.java`
- `ApplicationController.java`
- Form ứng tuyển với upload CV

---

## 🎯 Testing Checklist

### Danh sách việc làm
- [ ] Truy cập `/jobs` hiển thị đầy đủ
- [ ] Filter theo keyword works
- [ ] Filter theo location works
- [ ] Filter theo category works
- [ ] Filter theo employment type works
- [ ] Sort (newest, oldest, views) works
- [ ] Pagination works
- [ ] Save button works (với CANDIDATE role)
- [ ] View count hiển thị đúng

### Chi tiết việc làm
- [ ] Truy cập `/jobs/{id}` hiển thị đầy đủ
- [ ] Company logo fallback works
- [ ] Description render HTML đúng
- [ ] View count tăng mỗi lần xem
- [ ] Save button works
- [ ] Share buttons works
- [ ] Deadline warning hiển thị (nếu có)

### Hot jobs
- [ ] Truy cập `/jobs/hot` hiển thị đầy đủ
- [ ] Ranking badges (#1, #2...) hiển thị
- [ ] Filter theo range (7/30/90 ngày) works
- [ ] View count badge hiển thị
- [ ] HOT badge animation works

### Saved jobs
- [ ] Login với CANDIDATE role
- [ ] Truy cập `/jobs?saved=true`
- [ ] Save job từ danh sách
- [ ] Unsave job từ saved list
- [ ] Count badge trong header cập nhật

---

## 🐛 Troubleshooting

### Lỗi 404 khi truy cập /jobs
**Nguyên nhân:** SecurityConfig chưa permit `/jobs/**`
**Giải pháp:** Đã fix trong file SecurityConfig.java

### Lỗi "Table 'jobfinder.jobs' doesn't exist"
**Nguyên nhân:** Chưa chạy SQL script
**Giải pháp:** Import file `database_jobs_schema.sql`

### Save button không hoạt động
**Nguyên nhân:** Chưa login hoặc không có CANDIDATE role
**Giải pháp:** 
```sql
-- Kiểm tra user có role_id = 3 (CANDIDATE)
SELECT * FROM users WHERE email = 'your@email.com';
```

### View count không tăng
**Nguyên nhân:** Cột `last_viewed_at` null
**Giải pháp:** Đã handle trong `Job.incrementViewCount()`

---

## 🚀 Next Steps (Tùy chọn)

### 1. Application System
- Tạo form ứng tuyển
- Upload CV
- Track application status

### 2. Email Notifications
- Thông báo khi có job mới match
- Thông báo deadline sắp hết

### 3. Admin Dashboard
- Quản lý jobs
- Duyệt jobs
- Thống kê views, applications

### 4. Employer Features
- Đăng tin tuyển dụng
- Quản lý ứng viên
- Xem thống kê

---

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. ✅ Database đã import SQL script?
2. ✅ Application đang chạy trên port 8080?
3. ✅ SecurityConfig đã permit `/jobs/**`?
4. ✅ Sample data đã có trong database?

Chúc bạn thành công! 🎉
