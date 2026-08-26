# Frontend — arquitetura

App React + Vite + TypeScript. Contrato HTTP: [`../docs/FRONTEND_API_CONTRACT.md`](../docs/FRONTEND_API_CONTRACT.md).

## Pastas

| Pasta | Função |
|---|---|
| `src/pages` | Composição de rota (layout + seções) |
| `src/components/ui` | Primitivos visuais (`AppButton`, `AppCard`, `InlineAlert`, …) |
| `src/components/dashboard` | Dashboard do doador |
| `src/components/requests` | Pedidos do solicitante |
| `src/components/blood-center` | Busca de hemocentro |
| `src/components/register` / `profile` | Cadastro e perfil |
| `src/hooks` | Estado assíncrono da tela |
| `src/services` | Chamadas HTTP e normalização |
| `src/context` | `AuthContext` (sessão JWT) |
| `src/routes` | Tabela de rotas, guards e `roleRouting` |
| `src/types` | Tipos compartilhados (`auth`, `party`, …) |
| `src/api` | Cliente Axios (Bearer + `401`) |

## Rotas

| Path | Quem |
|---|---|
| `/`, `/login`, `/register` | Público |
| `/register/check-email`, `/confirm-email` | Confirmação de e-mail |
| `/dashboard`, `/dashboard/recommendations`, `/donations`, `/donations/external/new`, `/profile` | Autenticado (doador / admin) |
| `/requests`, `/requests/new` | Autenticado (solicitante) |
| `/blood-center` | Autenticado (hemocentro) |

`resolvePostLoginPath` manda requester-only para `/requests`, donor/admin para `/dashboard`, blood center para `/blood-center`.

## Fluxos

### Auth

1. `LoginPage` → `useAuth().login` → `authService` persiste JWT.
2. `RequireAuth` protege o restante.
3. Cliente HTTP injeta Bearer e trata `401` globalmente.
4. `"Email not confirmed"` → opção de reenviar confirmação.

### Cadastro

1. `RegisterForm` cria party (`POST /parties/persons` ou `/organizations`).
2. Se `emailConfirmationRequired`, vai para `/register/check-email` (não faz login).
3. Perfis escolhidos no form ficam em `pendingProfiles` até o primeiro login confirmado.
4. Confirmação: `/confirm-email?token=` → `POST /auth/confirm-email`.
5. Depois do login, aplicar papéis pendentes e **logar de novo** para o JWT trazer as roles.

### Doador

1. `DonorDashboardPage` + `useDonorDashboard`: resumo e recomendações.
2. Aceitar recomendação / agendar: `POST /donations` com `intendedDate` (+ `expectedTime` se houver slot).
3. Doação externa: `/donations/external/new`.

### Solicitante

1. `/requests` lista pedidos (`GET /donation-requests/{partyId}`) com progresso do snapshot.
2. Novo pedido: busca hemocentro (`GET /blood-centers/search`) e `POST /donation-requests`.

### Hemocentro

1. `/blood-center` + `useBloodCenterWorkspace`.
2. Estoque (`GET/PUT /blood-centers/inventory`), agenda (`GET/PUT /schedule`), slots e consultas (`GET /appointments`).

## Convenções

- Parse/normalização de API em `services`, não em pages.
- Pages só orquestram; estado de negócio nos hooks.
- Preferir primitivos de `components/ui` a botões/cards avulsos.
- Comentários só onde o fluxo não é óbvio.
