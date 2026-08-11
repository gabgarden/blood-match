# Frontend Architecture Guide

## Folder intent
- `src/pages`: route-level composition only (layout + section orchestration).
- `src/components/ui`: reusable visual primitives (`AppButton`, `AppCard`, `InlineAlert`).
- `src/components/dashboard`: dashboard feature components.
- `src/hooks`: feature hooks that own async orchestration and screen state.
- `src/services`: API integration and data normalization.
- `src/context`: global state providers (`AuthContext`).
- `src/routes`: route guard and route table.

## Core flows

### Auth flow
1. `LoginPage` calls `useAuth().login`.
2. `authService` persists JWT session.
3. Protected routes (`RequireAuth`) unlock donor dashboard.
4. `api` client injects bearer token and handles critical `401` globally.

### Register flow
1. `RegisterForm` creates party (`person` or `organization`).
2. Performs login immediately after successful register.
3. Optionally creates donor/requester profile(s).
4. Logs in again to refresh JWT roles.
5. Redirects to `/dashboard`.

### Donor dashboard flow
1. `DonorDashboardPage` computes role gates.
2. `useDonorDashboard` loads hero summary and recommendations.
3. Page renders only role-allowed sections.
4. Feature actions (`acceptDonation`) remain inside hook to keep page declarative.

## Current conventions
- Keep API response parsing/normalization in `services`, not in pages.
- Keep pages focused on composition; move business state to hooks.
- Use `components/ui` primitives before creating one-off button/card variants.
- Add short comments only where flow is non-obvious.

## Next recommended refactors
- Create `src/types/donorDashboard.ts` and move donor dashboard types out of hook/page files.
- Add `src/components/ui/AppInput.tsx` and migrate login/register inputs.
- Add route-level layouts (`AuthLayout`, `DonorDashboardLayout`) to remove duplicated shell code.
- Introduce tests for `authService` and `useDonorDashboard` with mocked API responses.
