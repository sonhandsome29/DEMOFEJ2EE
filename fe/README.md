# FE (React) - Backend Security Refactor Integration

## Run

```bash
cd fe
cp .env.example .env
npm install
npm run dev
```

## Backend connection

Set API base URL in `fe/.env`:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

FE reads this value in `src/api/http.js` via `import.meta.env.VITE_API_BASE_URL`.

## If FE shows "cannot connect"

1. Backend must run at `http://localhost:8080`.
2. Verify an endpoint quickly:
   - `http://localhost:8080/api/courses`
3. Restart FE after `.env` changes.
4. Ensure backend CORS allows `http://localhost:5173`.

## Implemented contract coverage

- Auth: register/login/forgot/reset/change-password
- Handles `429` + `Retry-After`
- Courses: categories + locked lessons (`videoUrl` can be null)
- Checkout: local cart only + payment methods (`CARD`, `MOMO`, `BANK_TRANSFER`)
- Admin orders: list and patch status with real APIs
