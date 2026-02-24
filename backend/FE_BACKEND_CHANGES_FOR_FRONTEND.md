# Backend Security Refactor - Frontend Integration Guide

This document is for FE developers only.

Scope: what backend changed, what FE must update, and how to test end-to-end safely.

---

## 1) What backend has been changed

The backend has been hardened and refactored in these areas:

1. Auth hardening
   - Password reset token is now stored as hash (not plaintext).
   - JWT now uses `tokenVersion` revocation logic.
   - After `change-password` or `reset-password`, old tokens become invalid.

2. Endpoint abuse protection
   - Added rate limiting on auth-related endpoints.
   - Returns HTTP `429` with `Retry-After` header.

3. API exposure hardening
   - Swagger/OpenAPI is disabled by default.
   - Swagger is enabled in `dev` profile only.

4. Course content protection
   - Public course detail now masks `videoUrl` for non-preview lessons.

5. Checkout authorization hardening
   - Checkout now only accepts `PUBLISHED` courses.

6. Input safety and request bounds
   - Added server-side text/url sanitization for course content.
   - Added pagination bounds validation (`size` max 100, `page` >= 0).

---

## 2) Breaking and important API contract changes

## 2.1 Auth

### Register
- Endpoint: `POST /api/auth/register`
- Request:

```json
{
  "fullName": "Alice Nguyen",
  "email": "alice@example.com",
  "password": "StrongPass123!"
}
```

- Password policy (required for register/reset/change):
  - 8-128 chars
  - at least 1 uppercase
  - at least 1 lowercase
  - at least 1 digit
  - at least 1 special char
  - no spaces

### Login
- Endpoint: `POST /api/auth/login`

### Forgot password
- Endpoint: `POST /api/auth/forgot-password`
- Request:

```json
{
  "email": "alice@example.com"
}
```

- Response data:
  - always includes message
  - `resetToken` is included only in local/dev/test when debug token exposure is enabled
  - FE must NOT depend on `resetToken` in production

### Reset password
- Endpoint: `POST /api/auth/reset-password`
- Request:

```json
{
  "token": "token-from-email-or-dev-response",
  "newPassword": "StrongPass123!"
}
```

### Change password (authenticated)
- Endpoint: `PUT /api/auth/change-password`
- Headers: `Authorization: Bearer <token>`
- Request:

```json
{
  "currentPassword": "OldPass123!",
  "newPassword": "NewStrongPass123!"
}
```

### Token invalidation behavior (important)
- After password reset/change, all old JWTs are revoked.
- FE must be ready to receive `401` for old token immediately.
- FE UX recommendation: after successful reset/change, force re-login flow.

---

## 2.2 Rate limits (new)

If too many requests from same IP, backend returns HTTP `429` + `Retry-After` header.

Current limits:
- `POST /api/auth/login`: 5 requests / 1 minute
- `POST /api/auth/register`: 5 requests / 10 minutes
- `POST /api/auth/forgot-password`: 3 requests / 15 minutes
- `POST /api/auth/reset-password`: 5 requests / 15 minutes
- `PUT /api/auth/change-password`: 5 requests / 15 minutes

FE must:
- Handle `429` explicitly.
- Read `Retry-After` and show cooldown message/timer.

---

## 2.3 Courses

### `GET /api/courses`
- Params: `page`, `size`, `search`, `category`, `sort`
- Constraints: `page >= 0`, `1 <= size <= 100`
- Response `data.courses` now returns **CourseSummaryResponse** (safe public DTO)

### `GET /api/courses/featured`
- Returns list of **CourseSummaryResponse**

### `GET /api/courses/{slug}`
- Returns **CourseDetailResponse**
- In `sections[].lessons[]`:
  - `videoUrl` is returned only when `isPreview = true`
  - non-preview lessons have `videoUrl = null` in public endpoint

### `GET /api/courses/id/{id}`
- Admin only

### `GET /api/courses/categories`
- Returns `data` as `string[]` categories

---

## 2.4 Orders / Checkout

### Checkout
- Endpoint: `POST /api/orders`
- Request:

```json
{
  "courseIds": ["course-id-1", "course-id-2"],
  "paymentMethod": "CARD"
}
```

- `paymentMethod` allowed values:
  - `CARD`
  - `MOMO`
  - `BANK_TRANSFER`

- `MOCK` is no longer accepted.

- Response is standard wrapper:

```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "id": "order-id",
    "status": "PENDING",
    "paymentMethod": "CARD",
    "items": [...]
  }
}
```

### Get orders
- `GET /api/orders/me`
- `GET /api/orders/{orderId}`

### Admin update order status
- Preferred: `PATCH /api/admin/orders/{id}/status` with JSON body:

```json
{
  "status": "COMPLETED"
}
```

---

## 2.5 Progress

Supported endpoints:
- `GET /api/progress/my-learning`
- `GET /api/progress/{courseId}`
- `POST /api/progress/complete` body `{ courseId, lessonId }`
- `GET /api/progress/position/{lessonId}`
- `POST /api/progress/position/{lessonId}` body `{ position }`

Compatibility endpoints still exist, but FE should use the list above.

---

## 3) Frontend files that must be updated

This section maps directly to current FE codebase.

## 3.1 `frontend/src/api/auth.api.js`

Add missing methods:
- `forgotPassword(data)` -> `POST /auth/forgot-password`
- `resetPassword(data)` -> `POST /auth/reset-password`
- `changePassword(data)` -> `PUT /auth/change-password`

Expected request fields:
- forgot: `{ email }`
- reset: `{ token, newPassword }`
- change: `{ currentPassword, newPassword }`

## 3.2 `frontend/src/stores/auth.store.js`

Enhance error handling:
- For `429`, show cooldown text from `Retry-After`.
- For password policy errors, show server message directly.

After successful password change/reset:
- Clear token/user local storage
- Redirect to login

## 3.3 `frontend/src/api/orders.api.js`

Current `cart` endpoints (`/cart`, `/cart/add`, etc.) do not exist in backend.

Action:
- Remove or deprecate these methods:
  - `getCart`
  - `addToCart`
  - `removeFromCart`
  - `clearCart`

Keep and use:
- `checkout`
- `getMyOrders`
- `getById`
- admin order methods

## 3.4 `frontend/src/stores/cart.store.js`

Current issues:
- Uses non-existing `/cart` APIs.
- Uses default payment method `MOCK` (invalid now).
- Reads checkout response as `response.data.orderId` (wrong path).

Required updates:
- Keep cart fully local in FE state.
- For checkout, use only `ordersApi.checkout({ courseIds, paymentMethod })`.
- Allowed payment values: `CARD`, `MOMO`, `BANK_TRANSFER`.
- Read order id from `response.data.data.id`.

## 3.5 `frontend/src/pages/public/CheckoutPage.vue`

Current page is still mock-driven.

Required updates:
- Replace simulated timeout with real `cartStore.checkout(...)` call.
- Update payment options to backend values:
  - `CARD`, `MOMO`, `BANK_TRANSFER`
- Remove `mock` payment option in production UI.

## 3.6 `frontend/src/stores/courses.store.js`

Fix categories parsing:
- Current: `categories.value = response.data`
- Correct: `categories.value = response.data.data || []`

For course detail page:
- Expect `videoUrl` may be `null` for non-preview lessons in public route.
- UI must handle locked lessons safely.

## 3.7 `frontend/src/pages/admin/OrdersPage.vue`

Current page uses static mock data.

Required updates:
- Replace with backend API integration:
  - `GET /api/admin/orders`
  - `PATCH /api/admin/orders/{id}/status`

## 3.8 Pagination params in FE

Backend enforces `size <= 100`.

Required updates:
- Ensure FE never sends `size` above 100.
- For admin tables, use `size` param name consistently (not `limit`).

---

## 4) Standard response shape FE should always use

Backend wrapper:

```json
{
  "success": true,
  "message": "optional",
  "data": {},
  "timestamp": "2026-..."
}
```

Validation error shape example:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "password": "Password must be 8-128 characters, include uppercase, lowercase, number, and special character, and contain no spaces"
  }
}
```

Rate limit error example:

```json
{
  "success": false,
  "message": "Too many requests, please try again later"
}
```

---

## 5) Local development notes for FE team

To use Swagger locally, backend must run with `dev` profile.

Example command:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.jvmArguments="-DJWT_SECRET=your-strong-secret-at-least-32-bytes -DJWT_EXPIRATION=86400000"
```

Swagger URL in dev:
- `http://localhost:8080/swagger-ui.html`

Production-like profile:
- Swagger is disabled by default.

---

## 6) FE QA checklist (must pass)

1. Register/login with strong password works.
2. Weak password is blocked with clear message.
3. Forgot password UI handles both generic success and rate limit (`429`).
4. Reset password with valid token works, old token session becomes invalid.
5. Change password works and user is forced to re-login.
6. Checkout rejects invalid payment method and accepts only allowed values.
7. Public course detail shows non-preview lessons as locked (no `videoUrl`).
8. Categories list loads from `response.data.data`.
9. Admin orders page works against real API (no mock data).
10. FE handles `401/403/404/429` gracefully with user-friendly messages.

---

## 7) Quick FE migration order (recommended)

1. Update API layer (`auth.api.js`, `orders.api.js`, `courses.api.js` parsing fixes).
2. Update stores (`auth.store.js`, `cart.store.js`, `courses.store.js`).
3. Update checkout UI and auth forms (forgot/reset/change).
4. Replace admin orders mock page with real API.
5. Run full manual regression using QA checklist above.

---

## 8) Project mode recommendation (for thesis / coursework)

Backend status right now: stable enough for project demo. No required backend changes for the next step.

Recommended approach: keep backend as-is and finish FE integration.

### 8.1 What FE should build next (no BE changes required)

1. Bulk create courses from CSV/JSON in Admin UI
   - Parse CSV/JSON on FE.
   - Map each row to existing `CourseRequest`.
   - Call `POST /api/admin/courses` per row (sequential or small concurrency like 3).
   - Show import report table: success/failed + error per row.

2. Video upload in Admin UI via external provider
   - Use Cloudinary upload widget (or YouTube unlisted upload flow).
   - Get final URL from provider and write into `lesson.videoUrl`.
   - Save course by existing admin create/update endpoints.

3. Keep cart local in FE
   - Remove dependency on `/cart` APIs (backend does not provide these endpoints).
   - On checkout, send only `courseIds` + valid `paymentMethod` to `POST /api/orders`.

### 8.2 Why this is best for coursework

- Fast delivery (no large backend redesign).
- Clear demo story: bulk import + video upload + publish + purchase.
- Lower risk before defense day.
- Still looks professional and realistic.
