---
name: nextjs-security-audit
description: Skill chuyên sâu tìm kiếm lỗ hổng bảo mật, lỗi logic, crash SSR/Hydration, rò rỉ dữ liệu và sai sót điều hướng trong các dự án Next.js (App Router & Pages Router). Kích hoạt khi người dùng yêu cầu audit, quét lỗi, kiểm tra an toàn thông tin hoặc tối ưu dự án Next.js.
---

# Quy Trình Kiểm Tra & Audit Lỗ Hổng Dự Án Next.js (Next.js Security & Bug Audit Guide)

Skill này hướng dẫn quy trình kiểm tra toàn diện một dự án Next.js nhằm phát hiện sớm các lỗ hổng bảo mật, lỗi logic routing, rủi ro crash trên môi trường máy chủ (SSR), và lỗi chuẩn hóa mã nguồn.

---

## 📋 1. Các Bước Thực Hiện Audit

1. **Quét Cấu Trúc Dự Án (Project Reconnaissance)**:
   - Đọc `package.json` để xác định phiên bản Next.js, React, và các thư viện phụ thuộc.
   - Liệt kê toàn bộ file trong `app/` (App Router) hoặc `pages/` (Pages Router), `components/`, `util/` / `lib/`, `middleware.ts`.
2. **Chạy Kiểm Tra Tĩnh (Static Analysis Check)**:
   - Chạy lệnh `npx tsc --noEmit` để kiểm tra lỗi TypeScript compile.
   - Chạy lệnh `npm run lint` hoặc `npx next lint`.
3. **Phân Tích Chi Tiết Mã Nguồn (Deep Source Code Inspection)**:
   - Kiểm tra các file quản lý Auth (`auth.ts`, `fetchClient.ts`, `middleware.ts`).
   - Kiểm tra các trang xử lý Form, đăng nhập, đăng ký, CRUD dữ liệu.
4. **Lập Báo Cáo Audit (Structured Audit Report)**:
   - Báo cáo phân loại rõ ràng theo mức độ nghiêm trọng: 🔴 Critical, 🟠 High, 🟡 Medium, 🔵 Code Smell.

---

## 🛡️ 2. Danh Mục Kiểm Tra Bảo Mật (Security Audit Checklist)

### 2.1 Rò rỉ Thông tin & Hardcoded Secrets
* **Kiểm tra**:
  - Có hardcode API Key, DB Connection String, JWT Secret, hoặc URL Backend (`http://localhost:...`) trong client-side code hay không.
  - Các biến môi trường nhạy cảm có bị đặt tiền tố `NEXT_PUBLIC_` nhầm lẫn không (vì `NEXT_PUBLIC_` sẽ bị đóng gói công khai vào bundle trình duyệt).
* **Quy tắc**: Chỉ dùng `NEXT_PUBLIC_` cho thông tin công khai (VD: public API endpoint).

### 2.2 Quản lý Token & XSS (Insecure Token Storage & Decoding)
* **Kiểm tra**:
  - Token JWT có đang lưu ở `localStorage` / `sessionStorage` không (dễ bị đánh cắp qua lỗ hổng XSS).
  - Code giải mã JWT phía Client có kiểm tra tính toàn vẹn (Signature) hay không, hoặc có dùng Regex thiếu cờ `/g` gây lỗi (`replace(/_/, '/')` vs `replace(/_/g, '/')`).
* **Khuyên dùng**: Sử dụng **HttpOnly Cookies** kết hợp Middleware Next.js để xác thực token.

### 2.3 Phân Quyền & Kiểm Soát Truy Cập (Authorization & Access Control)
* **Kiểm tra**:
  - Các trang quản trị (Admin) chỉ ẩn/hiện nút bấm ở UI hay có thực sự bảo vệ ở Server (Middleware / Server Component check).
  - Kiểm tra xem Role nhận về từ Token có bị sai kiểu dữ liệu không (ví dụ: Backend trả về Mảng `["ROLE_ADMIN"]` nhưng Frontend lại so sánh dạng Chuỗi `role === 'ROLE_ADMIN'`).

### 2.4 DangerouslySetInnerHTML & XSS
* **Kiểm tra**:
  - Có sử dụng `dangerouslySetInnerHTML` với dữ liệu do người dùng nhập mà chưa qua hàm làm sạch (Sanitization như `DOMPurify`) không.

---

## 🚀 3. Danh Mục Kiểm Tra Lỗi SSR & Hydration (SSR & Hydration Checklist)

### 3.1 Truy cập API Trình duyệt trên Server (`localStorage`, `window`, `document`)
* **Kiểm tra**:
  - Các đoạn code chạy trong Server Component hoặc ngoài `useEffect` có truy cập `localStorage`, `window`, `document`, `navigator` mà không kiểm tra `typeof window !== 'undefined'` hay không.
* **Hậu quả**: Gây crash ứng dụng ngay lập tức với lỗi `ReferenceError: localStorage is not defined`.

### 3.2 Hydration Mismatch (Lỗi không đồng bộ Server-Client HTML)
* **Kiểm tra**:
  - Sử dụng các giá trị bất định (`Math.random()`, `Date.now()`, `new Date().toLocaleTimeString()`) trực tiếp trong JSX mà không thông qua State / `useEffect`.
  - Cấu trúc HTML không hợp lệ (ví dụ: thẻ `<p>` chứa thẻ `<div>`, thẻ `<a>` lồng thẻ `<a>`).

---

## 🛣️ 4. Danh Mục Lỗi Logic & Routing (Routing & Logic Checklist)

### 4.1 Sai Cấu Trúc Dynamic Route (App Router mismatch)
* **Kiểm tra**:
  - Nếu thư mục có dạng `app/products/edit/[id]/page.tsx`, code chuyển hướng phải là `router.push('/products/edit/' + id)`.
  - **Lỗi phổ biến**: Dùng `router.push('/products/edit?id=' + id)` khi không có tệp `app/products/edit/page.tsx`, dẫn đến lỗi **404 Not Found**.

### 4.2 Xử lý Response API Rỗng hoặc Thất Bại
* **Kiểm tra**:
  - Khi gọi `fetch()`, có kiểm tra `response.ok` không.
  - Khi API trả về HTTP 204 No Content hoặc Body rỗng, việc gọi `response.json()` có bị bọc `try/catch` hoặc check `content-length` chuẩn xác để tránh lỗi `Unexpected end of JSON input` hay không.

---

## 📝 5. Mẫu Báo Cáo Kết Quả Audit (Standard Audit Template)

Khi thực hiện audit dự án Next.js, kết quả phải được trình bày theo cấu trúc chuẩn:

```markdown
### 🚨 Kết Quả Audit Dự Án Next.js

#### 🔴 Lỗi Nghiêm Trọng (Critical)
- **Tệp tin**: `[Đường dẫn file](file:///...)`
- **Mô tả**: Tóm tắt ngắn gọn lỗi.
- **Nguyên nhân**: Phân tích đoạn code vi phạm.
- **Giải pháp khắc phục**: Code minh họa sửa đổi.

#### 🟠 Lỗi Mức Độ Trung Bình (Medium)
- ...

#### 🟡 Khuyên Dùng & Tối Ưu (Optimization & Clean Code)
- ...
```
