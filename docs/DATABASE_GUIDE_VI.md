# Tài liệu cơ sở dữ liệu LMS Moodle-like

Tài liệu này mô tả schema MySQL v2 trong
`src/main/resources/db/mysql-schema.sql`. Database có 17 bảng, được tổ chức
quanh người dùng, khóa học và các hoạt động trong khóa học.

## 1. Nhìn nhanh luồng dữ liệu

```text
USERS ──< COURSE_MEMBERSHIPS >── COURSES ──< COURSE_SECTIONS ──< ACTIVITIES
  │                                      │                         │
  │                                      ├──< COURSE_ANNOUNCEMENTS │
  │                                      │                         ├── LESSONS
  │                                      │                         ├── ASSIGNMENTS
  │                                      │                         │      └──< SUBMISSIONS ── 0..1 GRADES
  │                                      │                         ├──< ACTIVITY_FILES >── FILES
  │                                      │                         └──< ACTIVITY_COMPLETIONS >── USERS
  └──< SYSTEM_ROLE_ASSIGNMENTS >── ROLES
```

Ký hiệu `──<` biểu thị một bản ghi cha có thể liên kết với nhiều bản ghi con.
Các bảng nối như `COURSE_MEMBERSHIPS` biến quan hệ nhiều-nhiều thành hai quan
hệ một-nhiều.

## 2. Danh sách bảng

| Bảng | Vai trò | Khóa chính | Liên kết chính |
|---|---|---|---|
| `users` | Tài khoản người dùng và hồ sơ cơ bản | `id` | Được tham chiếu bởi bảng vai trò, khóa học, bài nộp, điểm, tệp và thông báo |
| `roles` | Danh mục vai trò và phạm vi áp dụng | `id` | Dùng để gán quyền toàn hệ thống hoặc vai trò trong khóa học |
| `system_role_assignments` | Gán role toàn hệ thống, hiện dùng cho Admin | `(user_id, role_id)` | Nối `users` với role có scope `SYSTEM` |
| `course_categories` | Danh mục khóa học, hỗ trợ cây danh mục | `id` | Có thể có danh mục cha và nhiều khóa học |
| `courses` | Thông tin khóa học | `id` | Thuộc danh mục tùy chọn; có người tạo, thành viên, sections và thông báo |
| `course_memberships` | Thành viên và role của thành viên trong khóa | `(course_id, user_id)` | Nối `users`, `courses` và role có scope `COURSE` |
| `course_sections` | Các chương/chủ đề trong khóa học | `id` | Mỗi section thuộc một khóa và có nhiều hoạt động |
| `activities` | Thông tin chung, thứ tự và khả năng hiển thị của hoạt động | `id` | Mỗi activity nằm trong section; có chi tiết ở bảng con tùy loại |
| `lessons` | Nội dung chi tiết của hoạt động loại bài học | `activity_id` | PK đồng thời là FK tới `activities.id` |
| `assignments` | Cấu hình chi tiết bài tập | `activity_id` | PK đồng thời là FK tới `activities.id`; nhận nhiều lần nộp |
| `submissions` | Bài làm và từng lần thử của học viên | `id` | Thuộc một assignment, do một user nộp; có tối đa một grade |
| `grades` | Điểm và phản hồi của một lần nộp | `submission_id` | PK đồng thời là FK tới `submissions.id`; lưu người chấm và thời điểm công bố |
| `course_announcements` | Thông báo trong khóa học | `id` | Thuộc một khóa và có tác giả |
| `files` | Metadata của tệp, không lưu nội dung file trong DB | `id` | Liên kết tới hoạt động hoặc bài nộp qua các bảng nối |
| `activity_files` | Gắn tệp vào hoạt động, ví dụ tài liệu học tập | `(activity_id, file_id)` | Nối `activities` với `files` |
| `submission_files` | Gắn tệp học viên nộp vào bài làm | `(submission_id, file_id)` | Nối `submissions` với `files` |
| `activity_completions` | Trạng thái hoàn thành hoạt động của học viên | `(activity_id, user_id)` | Nối `activities` với `users` |

## 3. Các cột quan trọng theo nhóm bảng

Danh sách dưới đây tập trung vào những cột nhóm cần hiểu khi viết entity và
service. Các bảng đều có các khóa chính/ngoại và chỉ mục như thể hiện trong
schema SQL.

### Người dùng và quyền

| Bảng | Cột đáng chú ý |
|---|---|
| `users` | `email` duy nhất; `password_hash` chỉ lưu mật khẩu đã băm; `full_name`; `status` (`ACTIVE`, `SUSPENDED`, `DELETED`); `created_at`, `updated_at` |
| `roles` | `code` duy nhất (`ADMIN`, `TEACHER`, `STUDENT`); `name`; `scope` (`SYSTEM` hoặc `COURSE`) |
| `system_role_assignments` | `user_id`, `role_id`, `role_scope`, `assigned_at`; khóa ngoại ghép bảo đảm role được gán có scope `SYSTEM` |

### Danh mục, khóa học và thành viên

| Bảng | Cột đáng chú ý |
|---|---|
| `course_categories` | `parent_id` trỏ về chính bảng này để tạo cây danh mục; xóa danh mục cha đặt `parent_id` của con thành `NULL` |
| `courses` | `category_id` tùy chọn; `created_by`; `course_code` duy nhất; `status` (`DRAFT`, `PUBLISHED`, `ARCHIVED`); `start_at`, `end_at`; thời gian tạo/cập nhật |
| `course_memberships` | `course_id`, `user_id`, `role_id`, `role_scope`, `status` (`ACTIVE`, `SUSPENDED`, `WITHDRAWN`), `joined_at`, `updated_at`; một user chỉ có một role trong mỗi course theo khóa chính hiện tại |

### Nội dung và hoạt động học

| Bảng | Cột đáng chú ý |
|---|---|
| `course_sections` | `course_id`; `title`, `description`; `position` duy nhất trong cùng course; `status` (`VISIBLE`, `HIDDEN`) |
| `activities` | `section_id`, `created_by`; `activity_type` (`LESSON`, `ASSIGNMENT`, `RESOURCE`); `title`, `description`, `position`; `status` (`VISIBLE`, `HIDDEN`); `available_from`, `available_until` |
| `lessons` | `activity_id`; `content` |
| `assignments` | `activity_id`; `due_at`; `max_grade` lớn hơn 0; `allow_late_submission` |

### Nộp bài, chấm điểm và thông báo

| Bảng | Cột đáng chú ý |
|---|---|
| `submissions` | `assignment_id`, `student_id`; `attempt_number`; `text_content`; `status` (`DRAFT`, `SUBMITTED`, `LATE`); `submitted_at` nullable khi là bản nháp; `created_at`, `updated_at` |
| `grades` | `submission_id`; `graded_by`; `score` không âm; `feedback`; `graded_at`; `released_at` nullable khi chưa công bố |
| `course_announcements` | `course_id`, `author_id`; `title`, `content`; `status` (`DRAFT`, `PUBLISHED`); `published_at` nullable khi là bản nháp; `updated_at` |

### Tệp và tiến độ

| Bảng | Cột đáng chú ý |
|---|---|
| `files` | `uploaded_by`; `storage_key` duy nhất; `original_name`; `mime_type`; `size_bytes`; `uploaded_at`. Nội dung file nằm trong storage, không nằm ở cột DB |
| `activity_files` | `activity_id`, `file_id`; `purpose`; `position` |
| `submission_files` | `submission_id`, `file_id` |
| `activity_completions` | `activity_id`, `user_id`; `status` (`NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`); `completed_at` bắt buộc nếu trạng thái là `COMPLETED` |

## 4. Cách hiểu các quan hệ quan trọng

### Người dùng có role toàn hệ thống và role theo khóa

- `ADMIN` có scope `SYSTEM` và được gán qua `system_role_assignments`.
- `TEACHER` và `STUDENT` có scope `COURSE`, được gán qua
  `course_memberships`.
- Khóa ngoại ghép `(role_id, role_scope)` trỏ tới `(roles.id, roles.scope)`,
  giúp database không cho gán role `COURSE` vào bảng quyền hệ thống hoặc role
  `SYSTEM` vào membership khóa học.
- Cùng một người có thể dạy khóa này và học khóa khác. Role không được lưu
  thành một cột duy nhất trong `users`.

### Khóa học đến bài học và bài tập

Một course có nhiều sections; section có nhiều activities. `activities` giữ
những thuộc tính chung và `activity_type` xác định loại hoạt động. Bảng
`lessons` hoặc `assignments` giữ phần dữ liệu riêng. Loại `RESOURCE` dùng
`activity_files` để gắn tài liệu.

Database hiện không tự bảo đảm mỗi activity có đúng một bản ghi chi tiết loại
tương ứng. Service phải tạo/cập nhật activity và bảng chi tiết trong cùng một
transaction, đồng thời bảo đảm:

- `LESSON` có đúng một hàng trong `lessons`, không có hàng trong `assignments`.
- `ASSIGNMENT` có đúng một hàng trong `assignments`, không có hàng trong `lessons`.
- `RESOURCE` không có hàng trong hai bảng chi tiết trên.

### Bài tập đến bài nộp và điểm

- Mỗi assignment có thể nhận nhiều submissions.
- Một submission là một lần thử; cặp `(assignment_id, student_id,
  attempt_number)` là duy nhất.
- Bản nháp có `status = 'DRAFT'` và `submitted_at = NULL`; bài đã gửi có
  `status = 'SUBMITTED'` hoặc `LATE` và phải có `submitted_at`.
- Một submission có tối đa một grade hiện hành. `released_at = NULL` nghĩa là
  điểm chưa được công bố cho học viên.
- Cơ sở dữ liệu chưa thể so sánh `grades.score` với
  `assignments.max_grade`; service phải kiểm tra giới hạn này.

## 5. Những quy tắc MySQL đã bảo vệ

- Email, mã khóa học, mã role và storage key không trùng.
- Scope role hợp lệ và role scope phải khớp ở nơi được gán.
- Các trạng thái phải thuộc tập giá trị đã định nghĩa trong schema.
- Ngày kết thúc khóa học/hoạt động không được trước ngày bắt đầu.
- `attempt_number > 0`, `max_grade > 0`, `score >= 0`.
- Thời điểm gửi bài, công bố thông báo, hoàn thành hoạt động và công bố điểm
  phải nhất quán với trạng thái tương ứng.
- Thứ tự section trong một course và activity trong một section không trùng.

## 6. Quy tắc backend bắt buộc

Các quy tắc sau liên quan đến nhiều hàng/bảng; không chỉ dựa vào FK/CHECK:

1. Người tạo course, activity và announcement phải có quyền phù hợp trong
   phạm vi tương ứng.
2. Chỉ thành viên `STUDENT` đang `ACTIVE` của course mới được nộp bài thuộc
   course đó.
3. Chỉ giảng viên/quản trị viên có quyền trong course mới được chấm bài.
4. Điểm không được vượt quá `assignments.max_grade`.
5. Khi đăng ký, không tạo membership trùng; xử lý đúng trường hợp membership
   đã `WITHDRAWN` theo chính sách nhóm.
6. Chỉ cho xem điểm đã được công bố và thông báo đã được xuất bản.
7. Ghi các thao tác nhiều bảng (tạo activity với subtype, gửi bài kèm file,
   chấm và công bố điểm) trong transaction phù hợp.

Nên archive hoặc đổi trạng thái thay vì xóa người dùng/course đã có dữ liệu
học tập. Schema dùng `ON DELETE RESTRICT` ở dữ liệu quan trọng để tránh xóa
nhầm lịch sử bài nộp và điểm.

## 7. Giới hạn phạm vi hiện tại

Đây là schema cho các chức năng cốt lõi nhóm đã phân công, không phải bản sao
toàn bộ Moodle. Quiz/câu hỏi, forum, nhóm học, hộp thư, gradebook tổng hợp,
điều kiện hoàn thành nâng cao, nhiều role cùng lúc trong một course và lịch
sử nhiều lần ghi danh có thể được bổ sung bằng migration sau.

## 8. Quy ước khi code Java

- Cột `BIGINT UNSIGNED` ánh xạ sang Java `Long`; không dùng `long` nếu cần
  biểu diễn giá trị chưa lưu bằng `null`.
- `DECIMAL` ánh xạ sang `BigDecimal`; không dùng `double` cho điểm.
- `DATETIME` ánh xạ thường dùng `LocalDateTime`.
- Lưu enum dạng chuỗi (`EnumType.STRING`), không lưu ordinal số.
- Các bảng nối có khóa chính ghép (`course_memberships`,
  `system_role_assignments`, `activity_files`, `submission_files`,
  `activity_completions`) cần chọn cách ánh xạ JPA tương ứng, ví dụ
  `@EmbeddedId`/`@MapsId`, hoặc entity khóa ghép riêng.
- Các cột thời gian tự cập nhật và ràng buộc SQL cần được đồng bộ với mapping
  entity; không bật Hibernate tự sửa schema. Dùng
  `spring.jpa.hibernate.ddl-auto=validate`.

## 9. Tạo database hoặc nâng cấp

- Database mới: chạy `src/main/resources/db/mysql-schema.sql`.
- Database v1 đã tạo trước đó: backup rồi chạy một lần
  `src/main/resources/db/mysql-upgrade-v1-to-v2.sql`.
- Không chạy lại migration v1 → v2 và không kỳ vọng
  `CREATE TABLE IF NOT EXISTS` cập nhật bảng đã có.
- Xem thêm `docs/DB_SETUP.md` để biết cách cấu hình MySQL và Spring Boot.
