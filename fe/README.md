# FE (React) - Backend Security Refactor Integration

## Run

```bash
cd fe
npm install
npm run dev
```

## Backend connection

Set API base URL in `.env`:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

This FE integrates with backend contracts:
- Auth: register/login/forgot/reset/change-password
- Rate-limit 429 + Retry-After handling
- Courses: categories parsing from `response.data`, detail handles locked lessons (`videoUrl = null`)
- Checkout: local cart only, valid payment methods (`CARD`, `MOMO`, `BANK_TRANSFER`)
- Admin orders: load and patch status via real API
