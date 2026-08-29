# SmartShop

Multi-branch retail management system: POS, inventory, purchases, sales, returns, stock transfers, expenses and reporting.

## Architecture

| Layer     | Technology                                                                 |
|-----------|----------------------------------------------------------------------------|
| Backend   | Java 21, Spring Boot 3.4.4, Spring Security (JWT), Spring Data JPA, Flyway, PostgreSQL |
| Frontend  | React 18, TypeScript, Vite, MUI v5, TanStack Query, Zustand, Recharts, Axios |
| Testing   | JUnit 5 + MockMvc (backend), Vitest + React Testing Library (frontend)     |

```
smartshop-backend/    Spring Boot API
smartshop-frontend/   React SPA
docker-compose.yml    Local stack: postgres + backend + frontend
```

## Quick start with Docker

```bash
docker compose up --build
```

- Frontend: http://localhost
- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

The database is seeded with default roles via Flyway. Register the first account through the
frontend, then grant it the `SUPER_ADMIN` role directly in the database (no global role is
assigned on registration by design):

```sql
-- roles are pre-seeded; find the role id
INSERT INTO user_branch_roles (id, user_id, shop_id, branch_id, role_id, is_active, created_at, updated_at)
SELECT gen_random_uuid(), u.id, NULL, NULL, r.id, true, now(), now()
FROM users u, roles r
WHERE u.email = '<your-email>' AND r.name = 'SUPER_ADMIN';
```

## Local development

Backend (needs PostgreSQL running):

```bash
cd smartshop-backend
.\gradlew.bat bootRun        # Windows
./gradlew bootRun            # macOS/Linux
```

Frontend:

```bash
cd smartshop-frontend
npm install
npm run dev                  # http://localhost:5173, proxies /api to :8080
```

## Tests

```bash
cd smartshop-backend && .\gradlew.bat test     # runs on H2 in PostgreSQL mode, ddl-auto=validate
cd smartshop-frontend && npm test
cd smartshop-frontend && npm run build         # tsc -b + vite build
```

## Environment variables (backend)

| Variable                      | Default            | Purpose                        |
|-------------------------------|--------------------|--------------------------------|
| `DB_URL`                      | `jdbc:postgresql://localhost:5432/smartshop` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `smartshop` / `smartshop123` | DB credentials |
| `APP_JWT_SECRET`              | dev secret         | JWT signing secret (override in prod) |
| `APP_JWT_ACCESS_TOKEN_EXPIRATION` | `900000`      | Access token TTL (ms) |
| `APP_JWT_REFRESH_TOKEN_EXPIRATION` | `604800000` | Refresh token TTL (ms) |
| `CORS_ALLOWED_ORIGINS`        | `http://localhost:5173` | Allowed CORS origins |
| `ESEWA_ENABLED` / `KHALTI_ENABLED` | `false`   | Enable real payment gateways |

## Security model

- Stateless JWT auth; access token in `Authorization: Bearer`, refresh token in an
  httpOnly cookie scoped to `/api/v1/auth`.
- Authorization is role-based (`SUPER_ADMIN`, `SHOP_ADMIN`, `MANAGER`, `CASHIER`,
  `INVENTORY_STAFF`, `ACCOUNTANT`) and enforced at the service layer with a branch/shop
  scope guard (`BranchScopeGuard`).
- Passwords hashed with BCrypt (strength 12); login rate-limited per IP.

## Payments

- `CASH` is processed fully (change calculation stored on the sale).
- `CARD`, `ESEWA`, `KHALTI` and `BANK_TRANSFER` go through the `PaymentGateway` interface.
  The default `MockPaymentGateway` simulates success with a `MOCK-` reference; implement the
  interface and enable the relevant flag to plug in real gateways.

## Notes

- Database schema is versioned with Flyway (`V1`–`V11`) and JPA runs with
  `ddl-auto: validate`, so entities and migrations must stay in sync.
- Inventory uses pessimistic row locking on stock adjustments to prevent overselling.
- Sales/purchases/returns/transfers use business document numbers
  (`INV-`, `PO-`, `RT-`, `PR-`, `TR-`) generated per branch.