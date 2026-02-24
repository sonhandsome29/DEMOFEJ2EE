# White-box Security Audit Report (Initial Pass)

> **Phạm vi thực tế đã kiểm tra:** repository hiện tại chỉ chứa `README.md`, không có mã nguồn backend/frontend để thực hiện white-box audit ở mức dòng lệnh/dòng code.
>
> Vì vậy, báo cáo này gồm: (1) xác nhận hiện trạng kho mã, (2) reverse-spec từ tài liệu yêu cầu bạn cung cấp trong ticket, và (3) kế hoạch refactor bảo mật có thể triển khai ngay khi đồng bộ đầy đủ source code.

## 1. Tổng quan Kiến trúc

* **Mục đích App:** Nền tảng học trực tuyến có đăng ký/đăng nhập, quản lý khóa học, thanh toán đơn hàng, theo dõi tiến độ học, và trang quản trị.
* **Tech Stack:**
  * Backend (suy luận từ mô tả): Java + Spring Boot, JWT auth, profile `dev/prod`, REST API theo chuẩn response wrapper.
  * Frontend (suy luận từ mô tả): Vue (file `.vue`), store pattern (`*.store.js`), API layer (`*.api.js`).
* **Luồng dữ liệu quan trọng (3 luồng nhạy cảm nhất):**
  1. **Auth credential flow**: register/login/forgot/reset/change-password + JWT lifecycle + token revocation (`tokenVersion`).
  2. **Order/Checkout flow**: cart local FE -> `POST /api/orders` -> trạng thái đơn + thanh toán.
  3. **Course content access flow**: public course detail trả dữ liệu lesson có điều kiện (`videoUrl` chỉ cho preview) + phân quyền admin endpoint.

## 2. Báo cáo Rà soát Bảo mật 🛑

> **Lưu ý quan trọng:** Không thể liệt kê lỗ hổng theo file/line thực tế vì repository chưa chứa source code để quét. Các vấn đề dưới đây là **risk-based findings** dựa trên contract thay đổi backend bạn cung cấp.

### 🔴 Critical

* **🔴 Vấn đề:** Không thể xác minh triển khai security controls bằng white-box do thiếu source code.
* **📍 Vị trí:** Toàn bộ repository (chỉ có `README.md`).
* **💀 Tác động:** Có nguy cơ “security by assumption”: tài liệu nói đã harden nhưng không có bằng chứng thực thi (code/config/test). Điều này có thể bỏ sót lỗ hổng nghiêm trọng đang tồn tại trong production.

**🛡️ Fix nhanh:**
1. Đồng bộ đầy đủ source backend + frontend + file cấu hình (`application*.yml`, security config, migration scripts).
2. Bổ sung bộ test bảo mật tối thiểu (auth, authorization, rate-limit, input validation).
3. Tạo SBOM và dependency scan trong CI.

### 🟠 High

* **🔴 Vấn đề:** Nguy cơ IDOR/authorization bypass trên các endpoint nhạy cảm (`/api/orders/{orderId}`, `/api/progress/{courseId}`, admin routes).
* **📍 Vị trí:** Chưa có file để định vị dòng code.
* **💀 Tác động:** User có thể truy cập đơn hàng/tiến độ của người khác nếu chỉ kiểm tra sự tồn tại của ID mà thiếu ownership check.

**🛡️ Fix nhanh:**
* Áp dụng ownership enforcement ở service layer cho mọi endpoint theo ID người dùng.
* Viết integration tests cho các case truy cập chéo tenant/user.

* **🔴 Vấn đề:** Nguy cơ bypass password/token policy ở các nhánh xử lý auth.
* **📍 Vị trí:** Chưa có file để định vị dòng code.
* **💀 Tác động:** Nếu validation không đồng nhất giữa register/reset/change-password, attacker có thể tạo password yếu hoặc reuse token cũ.

**🛡️ Fix nhanh:**
* Gom policy password vào một validator dùng chung.
* Sau reset/change-password: increment `tokenVersion`, revoke refresh/access tokens, force re-login.

### 🟡 Medium

* **🔴 Vấn đề:** Rủi ro xử lý lỗi/response không nhất quán phía FE (401/403/404/429).
* **📍 Vị trí:** Chưa có file FE để định vị.
* **💀 Tác động:** UX sai lệch, rò rỉ thông tin nội bộ qua thông báo lỗi thô, hoặc retry hành vi gây lock/rate-limit nặng hơn.

**🛡️ Fix nhanh:**
* Chuẩn hóa error interceptor FE, parse `Retry-After`, hiển thị thông điệp người dùng an toàn.

* **🔴 Vấn đề:** Rủi ro truyền tham số phân trang vượt ngưỡng (`size > 100`) gây tấn công tài nguyên.
* **📍 Vị trí:** Chưa có file FE/backend để định vị.
* **💀 Tác động:** Tăng tải DB/API, suy giảm hiệu năng.

**🛡️ Fix nhanh:**
* Clamp `size` ở FE + validate chặt ở BE + log/audit request vi phạm.

## 3. Điểm mù & Rủi ro

1. **Thiếu bằng chứng kiểm chứng:** không có source, test, CI pipeline nên không thể xác thực các cam kết hardening.
2. **Thiếu threat model chính thức:** chưa thấy mô hình actor, trust boundary, data classification.
3. **Chưa xác minh secret management:** không rõ có hardcoded secret hoặc quản lý biến môi trường sai cách không.
4. **Chưa xác minh dependency risk:** không có lockfile/pom/gradle/package manifest để scan CVE.
5. **Chưa xác minh logging/monitoring:** không rõ có audit log cho auth/order/admin action và cảnh báo bất thường.

## 4. Kế hoạch Refactor Chi tiết 🛠️

### Giai đoạn 1: Vá lỗi khẩn cấp (Hotfixes)

1. Đồng bộ source code đầy đủ vào repo này (backend, frontend, infra config).
2. Chạy security baseline ngay:
   * SAST (Semgrep/CodeQL)
   * SCA (OWASP Dependency-Check, npm audit)
   * Secret scan (gitleaks/trufflehog)
3. Khoá ngay các điểm auth/order/progress bằng test bắt buộc:
   * Invalid JWT sau change/reset password.
   * IDOR negative tests cho order/progress/admin.
   * Rate-limit tests trả 429 + `Retry-After`.

### Giai đoạn 2: Củng cố Kiến trúc (Hardening)

1. **Auth architecture:**
   * Chuẩn hóa token revocation strategy (tokenVersion + refresh token rotation).
   * Đồng nhất password policy validator dùng chung cho mọi endpoint auth.
2. **Authorization architecture:**
   * Chính sách “deny by default”, method-level security rõ ràng.
   * Ownership checks tại service layer (không chỉ controller).
3. **Input & output security:**
   * Sanitization + validation tập trung; reject malformed URLs/HTML.
   * DTO public/private tách rõ để tránh lộ field nhạy cảm (`videoUrl` non-preview).
4. **Operational security:**
   * Structured audit logging cho auth, checkout, admin status updates.
   * Alerting cho brute force/rate-limit abuse.

### Giai đoạn 3: Dọn dẹp (Cleanup)

1. Chuẩn hóa convention API layer/store FE theo backend wrapper `{ success, message, data }`.
2. Loại bỏ toàn bộ mock/cart API không tồn tại ở backend.
3. Chuẩn hóa error handling và mã lỗi domain.
4. Tạo Security Regression Checklist + chạy trong CI trước khi release.

---

## Phụ lục: Bằng chứng khám phá repository

* Repo hiện có duy nhất file `README.md` với nội dung tiêu đề dự án.
* Không tìm thấy thư mục `backend/`, `frontend/`, file build (`pom.xml`, `package.json`), hoặc mã nguồn để quét white-box.
