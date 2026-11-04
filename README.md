# Mini API Tuyển Dụng (Job Portal API)

Dự án RESTful API quản lý tuyển dụng xây dựng với **Java 17**, **Spring Boot 3**, **PostgreSQL 16**, **Flyway Migration** và đóng gói hoàn chỉnh bằng **Docker Compose**.

---

## 1. Công nghệ sử dụng

- **Ngôn ngữ**: Java 17
- **Framework**: Spring Boot 3.3.5 (`Spring Web`, `Spring Data JPA`, `Bean Validation`)
- **Cơ sở dữ liệu**: PostgreSQL 16
- **Migration**: Flyway (`V1__init_schema.sql`)
- **Kiểm thử**: JUnit 5, Mockito, Spring MockMvc, H2 In-Memory (16 Unit Tests tầng Service + 1 End-to-End Integration Test)
- **Đóng gói**: Docker (Multi-stage build với Eclipse Temurin 17) & Docker Compose

---

## 2. Kiến trúc & Thiết kế OOP

```text
src/main/java/com/jobportal/
├── controller/       # Tầng REST Controller (CompanyController, JobController, ApplicationController)
├── dto/              # Java Records bất biến cho Request/Response + Jakarta Validation
├── entity/           # JPA Entities kế thừa BaseEntity (Company, Job, Application, JobStatus)
├── exception/        # Ngoại lệ nghiệp vụ & GlobalExceptionHandler trả JSON thống nhất (400, 404, 409)
├── repository/       # Spring Data JPA Repositories & JobSpecification (Dynamic Query + chống N+1)
└── service/          # Interface & Implementation xử lý logic nghiệp vụ, quản lý Transaction
    └── impl/
```

### Điểm nhấn kỹ thuật:
- **OOP & DRY**: Lớp trừu tượng [`BaseEntity`](src/main/java/com/jobportal/entity/BaseEntity.java) đóng gói `id`, `createdAt`, `updatedAt` và lifecycle hooks (`@PrePersist`, `@PreUpdate`). Thực thể `Job` đóng gói hành vi kiểm tra trạng thái mở (`job.isOpen()`).
- **Tối ưu truy vấn (Chống N+1 Query)**: Sử dụng `@EntityGraph(attributePaths = "company")` và `root.fetch("company", JoinType.LEFT)` trong `JobSpecification` để lấy thông tin `Job` kèm `Company` chỉ trong **1 câu truy vấn SQL JOIN**.
- **Chống Race Condition trùng Email**: Chuẩn hoá email về chữ thường (`lowercase`), kiểm tra ở tầng Service kết hợp ràng buộc `UNIQUE (job_id, email)` ở tầng Database và bắt `DataIntegrityViolationException` trả về `409 Conflict`.

---

## 3. Hướng dẫn chạy dự án

### Cách 1: Chạy bằng Docker Compose (Khuyên dùng)

Yêu cầu máy đã cài đặt **Docker** và **Docker Compose**.

```bash
# Khởi tạo PostgreSQL 16, tự động chạy Flyway migration và khởi chạy ứng dụng Spring Boot
docker compose up -d --build
```

- Ứng dụng lắng nghe tại: `http://localhost:8080`
- PostgreSQL lắng nghe tại cổng host: `localhost:5434` (nội bộ Docker: `postgres:5432`, tránh xung đột nếu máy đã cài sẵn PostgreSQL ở cổng `5432`).
- Kiểm tra log ứng dụng:
  ```bash
  docker compose logs -f app
  ```
- Dừng hệ thống:
  ```bash
  docker compose down
  ```

### Cách 2: Chạy trực tiếp bằng Maven trên máy Local

Yêu cầu **JDK 17+** và **Maven 3.9+**.

1. Khởi chạy PostgreSQL (hoặc chỉ bật container DB từ Docker Compose):
   ```bash
   docker compose up -d postgres
   ```
2. Chạy ứng dụng Spring Boot (Flyway sẽ tự động tạo bảng khi khởi động):
   ```bash
   mvn spring-boot:run
   ```

### Chạy bộ Kiểm thử (Unit Tests & Integration Tests)

```bash
mvn test
```

---

## 4. Thiết kế bảng & Script Migration

File migration tự động chạy bởi Flyway tại: [`src/main/resources/db/migration/V1__init_schema.sql`](src/main/resources/db/migration/V1__init_schema.sql)

```sql
-- Create companies table
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    website VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create jobs table
CREATE TABLE jobs (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    salary_min NUMERIC(15, 2) NOT NULL CHECK (salary_min >= 0),
    salary_max NUMERIC(15, 2) NOT NULL CHECK (salary_max >= salary_min),
    location VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'CLOSED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for jobs filtering and sorting performance
CREATE INDEX idx_jobs_company_id ON jobs(company_id);
CREATE INDEX idx_jobs_status_location ON jobs(status, location);
CREATE INDEX idx_jobs_created_at ON jobs(created_at DESC);

-- Create applications table
CREATE TABLE applications (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    cv_link VARCHAR(500) NOT NULL,
    applied_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_applications_job_email UNIQUE (job_id, email)
);

CREATE INDEX idx_applications_job_id ON applications(job_id);
```

---

## 5. Danh sách API & Ví dụ Request / Response

### 5.1. Quản lý Công ty (`Company`)

#### Tạo mới Công ty (`POST /companies`)
```bash
curl -X POST http://localhost:8080/companies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "FPT Software",
    "address": "Khu Công nghệ cao Hòa Lạc, Hà Nội",
    "website": "https://fptsoftware.com"
  }'
```
**Response (`201 Created`)**:
```json
{
  "id": 1,
  "name": "FPT Software",
  "address": "Khu Công nghệ cao Hòa Lạc, Hà Nội",
  "website": "https://fptsoftware.com",
  "createdAt": "2026-11-04T20:10:00.123456+07:00",
  "updatedAt": "2026-11-04T20:10:00.123456+07:00"
}
```

#### Lấy danh sách Công ty (`GET /companies`)
```bash
curl -X GET http://localhost:8080/companies
```

#### Lấy chi tiết Công ty (`GET /companies/{id}`)
```bash
curl -X GET http://localhost:8080/companies/1
```

#### Cập nhật Công ty (`PUT /companies/{id}`)
```bash
curl -X PUT http://localhost:8080/companies/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "FPT Software Global",
    "address": "Cầu Giấy, Hà Nội",
    "website": "https://fptsoftware.com"
  }'
```

#### Xoá Công ty (`DELETE /companies/{id}`)
```bash
curl -X DELETE http://localhost:8080/companies/1
```

---

### 5.2. Quản lý Tin tuyển dụng (`Job`)

#### Tạo mới Job (`POST /jobs`)
```bash
curl -X POST http://localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Java Spring Boot Backend Developer",
    "description": "Phát triển hệ thống Microservices với Java 17, Spring Boot 3 và PostgreSQL",
    "salaryMin": 1500.00,
    "salaryMax": 3000.00,
    "location": "Ha Noi",
    "status": "OPEN",
    "companyId": 1
  }'
```
**Response (`201 Created`)**:
```json
{
  "id": 1,
  "title": "Java Spring Boot Backend Developer",
  "description": "Phát triển hệ thống Microservices với Java 17, Spring Boot 3 và PostgreSQL",
  "salaryMin": 1500.00,
  "salaryMax": 3000.00,
  "location": "Ha Noi",
  "status": "OPEN",
  "companyId": 1,
  "companyName": "FPT Software",
  "createdAt": "2026-11-04T20:11:00.123456+07:00",
  "updatedAt": "2026-11-04T20:11:00.123456+07:00"
}
```

#### Tìm kiếm, Lọc, Phân trang & Sắp xếp Job (`GET /jobs`)
Hỗ trợ các query parameters:
- `keyword`: Tìm theo từ khoá trong `title` hoặc `description` (không phân biệt hoa/thường)
- `location`: Lọc theo địa điểm (không phân biệt hoa/thường)
- `status`: Lọc theo trạng thái (`OPEN` hoặc `CLOSED`)
- `page`: Số trang (bắt đầu từ `0`, mặc định `0`)
- `size`: Số bản ghi mỗi trang (mặc định `10`)
- `sort`: Trường sắp xếp và chiều sắp xếp (ví dụ: `salaryMax,desc` hoặc `createdAt,desc`)

```bash
curl -X GET "http://localhost:8080/jobs?keyword=Spring&location=Ha%20Noi&status=OPEN&page=0&size=5&sort=salaryMax,desc"
```
**Response (`200 OK`)**:
```json
{
  "content": [
    {
      "id": 1,
      "title": "Java Spring Boot Backend Developer",
      "description": "Phát triển hệ thống Microservices với Java 17, Spring Boot 3 và PostgreSQL",
      "salaryMin": 1500.00,
      "salaryMax": 3000.00,
      "location": "Ha Noi",
      "status": "OPEN",
      "companyId": 1,
      "companyName": "FPT Software",
      "createdAt": "2026-11-04T20:11:00.123456+07:00",
      "updatedAt": "2026-11-04T20:11:00.123456+07:00"
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

#### Lấy chi tiết Job (`GET /jobs/{id}`)
```bash
curl -X GET http://localhost:8080/jobs/1
```

#### Cập nhật Job (`PUT /jobs/{id}`)
```bash
curl -X PUT http://localhost:8080/jobs/1 \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Senior Java Spring Boot Developer",
    "description": "Cập nhật mô tả công việc",
    "salaryMin": 2000.00,
    "salaryMax": 3500.00,
    "location": "Ha Noi",
    "status": "CLOSED",
    "companyId": 1
  }'
```

#### Xoá Job (`DELETE /jobs/{id}`)
```bash
curl -X DELETE http://localhost:8080/jobs/1
```

---

### 5.3. Ứng tuyển (`Application`)

#### Nộp đơn ứng tuyển vào Job (`POST /jobs/{id}/applications`)
```bash
curl -X POST http://localhost:8080/jobs/1/applications \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Trần Văn Mạnh",
    "email": "tranvanmanh@example.com",
    "cvLink": "https://example.com/cv/tran-van-manh.pdf"
  }'
```
**Response (`201 Created`)**:
```json
{
  "id": 1,
  "jobId": 1,
  "jobTitle": "Java Spring Boot Backend Developer",
  "fullName": "Trần Văn Mạnh",
  "email": "tranvanmanh@example.com",
  "cvLink": "https://example.com/cv/tran-van-manh.pdf",
  "appliedAt": "2026-11-04T20:12:00.123456+07:00"
}
```

#### Xem danh sách hồ sơ đã nộp của một Job (`GET /jobs/{id}/applications`)
```bash
curl -X GET http://localhost:8080/jobs/1/applications
```

---

### 5.4. Cấu trúc JSON lỗi thống nhất (`400`, `404`, `409`)

#### Lỗi `400 Bad Request` (Dữ liệu đầu vào không hợp lệ)
```json
{
  "timestamp": "2026-11-04T20:13:00.000+07:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for request payload",
  "path": "/jobs/1/applications",
  "fieldErrors": {
    "fullName": "Applicant full name is required",
    "email": "Email must be a valid email address",
    "cvLink": "CV link must be a valid HTTP or HTTPS URL"
  }
}
```

#### Lỗi `400 Bad Request` (Nộp đơn vào Job đã `CLOSED`)
```json
{
  "timestamp": "2026-11-04T20:13:15.000+07:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Cannot submit application because job with id 1 is CLOSED",
  "path": "/jobs/1/applications"
}
```

#### Lỗi `404 Not Found` (Không tìm thấy Company hoặc Job)
```json
{
  "timestamp": "2026-11-04T20:13:30.000+07:00",
  "status": 404,
  "error": "Not Found",
  "message": "Job with id 999 was not found",
  "path": "/jobs/999"
}
```

#### Lỗi `409 Conflict` (Trùng Email ứng tuyển trong cùng một Job)
```json
{
  "timestamp": "2026-11-04T20:14:00.000+07:00",
  "status": 409,
  "error": "Conflict",
  "message": "Email 'tranvanmanh@example.com' has already applied for job with id 1",
  "path": "/jobs/1/applications"
}
```
