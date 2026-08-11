# BloodMatch — Contrato da API (Frontend)

Documento de referência do **backend atual** (Spring Boot, JWT, sem prefixo `/api` e sem versionamento de URL).

Use este arquivo no repositório do frontend para alinhar clientes, services, stores e rotas com o contrato real da API.

---

## Prompt sugerido (colar no chat do projeto frontend)

```text
Leia o arquivo FRONTEND_API_CONTRACT.md (contrato atual do backend BloodMatch).
O frontend está desatualizado em relação a este contrato.

Faça todas as alterações necessárias para alinhar o frontend ao backend:
- base URL, paths, métodos HTTP e payloads
- autenticação JWT (Bearer), persistência de token/partyId/roles
- re-login obrigatório após registrar papéis (DONOR / REQUESTER / BLOOD_CENTER)
- ownership: partyId/personId/organizationId do request devem ser os do usuário logado
- tratamento de erros { "error": "..." } com status 400/401/403/404/409
- breaking changes (ex.: /requests/* → /donation-requests/*)
- CORS / PATCH se houver chamadas browser
- UI/fluxos que dependem de roles

Não invente endpoints. Siga apenas este contrato. Ao terminar, liste o que mudou.
```

---

## 1. Visão geral

| Item | Valor |
|------|--------|
| Base URL (local) | `http://localhost:8080` |
| Prefixo `/api` | **Não** — paths na raiz |
| Versionamento de URL | **Não** |
| Auth | JWT Bearer, stateless |
| Content-Type | `application/json` |
| Datas | ISO date `YYYY-MM-DD` |
| Docs interativas | `/swagger-ui.html`, `/v3/api-docs` |

### Header de autenticação

```http
Authorization: Bearer <accessToken>
```

### CORS (browser)

Origens permitidas:

- `http://localhost:5173` (Vite)
- `https://gabgarden.github.io/`

Métodos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`  
Credentials: `true`

---

## 2. Auth, roles e ownership

### 2.1 Login

`POST /auth/login` — **público**

Request:

```json
{
  "email": "user@email.com",
  "password": "senha"
}
```

Response `200`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "roles": ["DONOR"],
  "partyId": "<uuid>"
}
```

Persistir no frontend: `accessToken`, `partyId`, `roles`, `expiresIn`.

### 2.2 Roles (`SecurityRole`)

| Role | Significado |
|------|-------------|
| `DONOR` | Doador |
| `REQUESTER` | Solicitante de sangue |
| `BLOOD_CENTER` | Hemocentro (registrável; quase sem rotas gated por esse role hoje) |
| `SYSTEM_ADMIN` | Admin (bypass de ownership; não há fluxo público para obter) |

Roles no JWT **não** usam prefixo `ROLE_`.

### 2.3 Fluxo típico de conta

1. Registrar pessoa/org (`POST /parties/persons` ou `/parties/organizations`) → recebe `id` da party.
2. Login → JWT com `partyId` e `roles` (inicialmente vazios ou só o que já existir).
3. Registrar papel (`POST /donors`, `/requesters` ou `/blood-centers`).
4. **Login de novo** — o backend **não** emite token novo no registro de papel. Sem re-login, rotas gated por role retornam **403**.
5. Não há endpoint de refresh token.

### 2.4 Ownership (obrigatório)

O frontend **não** deve permitir escolher arbitrariamente o id de outra party.

Regras:

1. Campos `partyId` / `personId` / `organizationId` (quando representam *a própria* conta) devem ser o `partyId` do JWT.
2. Operações por `requestId` / `donationId` só funcionam se o recurso for do usuário autenticado (ou admin).
3. Violação → **403** `{ "error": "Forbidden" }`.

Na prática: após login, use sempre `auth.partyId` nos bodies/paths/queries de “eu mesmo”.

---

## 3. Erros

Corpo padrão:

```json
{ "error": "<mensagem>" }
```

| Status | Quando |
|--------|--------|
| `400` | Validação / regra de negócio inválida |
| `401` | Sem token, token inválido/expirado, credenciais erradas |
| `403` | Role insuficiente ou ownership |
| `404` | Recurso não encontrado |
| `409` | Conflito (ex.: papel já registrado) |

Mensagens comuns de auth:

- `"Unauthorized"` — sem autenticação
- `"Invalid or expired token"`
- `"Forbidden"`
- `"Invalid credentials"`
- `"User account is disabled"`

O frontend deve tratar `401` (logout / ir para login) e `403` (mensagem de permissão / fluxo incompleto de role).

---

## 4. Enums e valores aceitos

### Blood type

`A+` | `A-` | `B+` | `B-` | `AB+` | `AB-` | `O+` | `O-`

### Urgency (pedido de doação)

`LOW` | `MEDIUM` | `CRITICAL`

### Donation status (respostas)

`PENDING` | `COMPLETED` | `CANCELLED` (e possivelmente `UNKNOWN` só em edge cases)

### Party type (registro)

- Pessoa → `type`: `"PERSON"`
- Organização → `type`: `"ORGANIZATION"`

---

## 5. Inventário completo de endpoints

Legenda de auth:

- **Public** — sem token
- **Auth** — qualquer JWT válido
- **Role** — JWT com a(s) role(s) listada(s)
- **Own** — id da própria party / ownership do recurso

---

### 5.1 Auth

#### `POST /auth/login` — Public

| | |
|--|--|
| Body | `email`, `password` (string, obrigatórios) |
| `200` | `accessToken`, `tokenType`, `expiresIn`, `roles[]`, `partyId` |

---

### 5.2 Parties

#### `POST /parties/persons` — Public → `201`

Body:

| Campo | Tipo | Obrigatório |
|-------|------|-------------|
| `name` | string | sim |
| `phoneNumber` | string | sim |
| `cpf` | string | sim |
| `birthDate` | date | sim |
| `email` | string | sim |
| `password` | string | sim |
| `passwordConfirmation` | string | sim |
| `street` | string | não* |
| `city` | string | não* |
| `state` | string | não* |
| `zipCode` | string | não* |

\* Se qualquer campo de endereço for enviado, **todos** os quatro devem ser preenchidos.

Response: `{ "id": "<uuid>", "type": "PERSON" }`

#### `POST /parties/organizations` — Public → `201`

Body: `name`, `phoneNumber`, `cnpj`, `email`, `password`, `passwordConfirmation`, endereço opcional (mesma regra all-or-nothing).

Response: `{ "id": "<uuid>", "type": "ORGANIZATION" }`

#### `PATCH /parties/name` — Auth + Own(`partyId`) → `200`

```json
{ "partyId": "<uuid>", "newName": "Novo nome" }
```

Response: `{ "id": "<uuid>", "name": "Novo nome" }`

---

### 5.3 Roles / doador

#### `POST /donors` — Auth + Own(`personId`) → `201`

```json
{
  "personId": "<partyId do JWT>",
  "bloodType": "O+",
  "weight": 70.5
}
```

Response: `{ "id": "<donorRoleId>" }`  
**Depois: re-login.**

#### `POST /requesters` — Auth + Own(`partyId`) → `201`

```json
{ "partyId": "<partyId do JWT>" }
```

Response: `{ "id": "<requesterRoleId>" }`  
**Depois: re-login.**

#### `POST /blood-centers` — Auth + Own(`organizationId`) → `201`

```json
{ "organizationId": "<partyId do JWT>" }
```

Response: `{ "id": "<bloodCenterRoleId>" }`  
**Depois: re-login.**  
Obs.: hoje nenhuma rota de negócio exige autoridade `BLOOD_CENTER` no `SecurityConfig`.

#### `PATCH /donors/profile` — Auth + Own(`personId`) → `200`

```json
{
  "personId": "<uuid>",
  "bloodType": "A+",
  "weight": 72.0
}
```

Response: `{ "id": "<donorRoleId>" }`

#### `PATCH /donors/recommendation-distance` — Role `DONOR` + Own(`personId`) → `200`

```json
{
  "personId": "<uuid>",
  "maxDistanceInKm": 30
}
```

Response: `{ "personId": "<uuid>", "maxDistanceInKm": 30 }`

#### `GET /donors/{personId}/summary` — Auth + Own(path) → `200`

```json
{
  "personId": "<uuid>",
  "donorName": "string",
  "phoneNumber": "string",
  "bloodType": "O+",
  "address": "string",
  "lastDonationDate": "2026-01-15",
  "daysRemaining": 0,
  "livesImpacted": 0
}
```

`lastDonationDate` pode ser `null`.

#### `GET /donors/{personId}/donations` — Auth + Own(path) → `200`

Array:

```json
[
  {
    "donationId": "<uuid>",
    "date": "2026-01-15",
    "location": "Nome do hemocentro"
  }
]
```

---

### 5.4 Donation requests

> **Breaking change:** paths antigos `/requests/recommendations` e `/requests/{id}/notify` foram unificados sob `/donation-requests/...`.

#### `POST /donation-requests` — Role `REQUESTER` + Own(`partyId`) → `201`

```json
{
  "partyId": "<requester partyId = JWT>",
  "organizationId": "<uuid do hemocentro>",
  "bloodTypeNeeded": "A+",
  "goalBloodBags": 3,
  "dateLimit": "2026-12-31",
  "urgency": "MEDIUM",
  "directedTo": "opcional"
}
```

`goalBloodBags` deve ser **> 0**.  
`organizationId` é o hemocentro destino (não precisa ser o JWT).

Response: `{ "id": "<requestId>" }`

#### `GET /donation-requests/recommendations?personId=` — Role `DONOR` + Own(query) → `200`

Query obrigatória: `personId` (= JWT `partyId`).

Item:

```json
{
  "requestId": "<uuid>",
  "bloodTypeNeeded": "A+",
  "dateLimit": "2026-12-31",
  "bloodCenterName": "string",
  "urgency": "MEDIUM",
  "distanceInKm": 12.5,
  "goalBloodBags": 3,
  "fulfilledBloodBags": 1,
  "goalReached": false
}
```

#### `GET /donation-requests/{partyId}` — Role `REQUESTER` + Own(path) → `200`

Lista de pedidos do solicitante:

```json
[
  {
    "requestId": "<uuid>",
    "bloodTypeNeeded": "A+",
    "dateRequested": "2026-01-01",
    "dateLimit": "2026-12-31",
    "active": true,
    "expired": false,
    "bloodCenterName": "string",
    "bloodCenterPhoneNumber": "string",
    "urgency": "MEDIUM",
    "goalBloodBags": 3,
    "fulfilledBloodBags": 1,
    "remainingBloodBags": 2,
    "goalReached": false
  }
]
```

Cuidado: não use o literal `recommendations` como `{partyId}`.

#### `DELETE /donation-requests/{requestId}` — Role `REQUESTER` + Own(recurso) → `204`

Sem body.

#### `POST /donation-requests/{id}/notify` — Role `REQUESTER` + Own(recurso) → `200`

Sem body.

Response: `{ "message": "Notifications sent to eligible donors successfully." }`

#### `PATCH /donation-requests/date-limit` — Role `REQUESTER` + Own(recurso) → `200`

```json
{
  "requestId": "<uuid>",
  "newDateLimit": "2026-12-31"
}
```

`newDateLimit` não pode ser no passado.

Response: `{ "id": "<uuid>", "dateLimit": "2026-12-31" }`  
(`dateLimit` vem como **string**)

#### `PATCH /donation-requests/goal-blood-bags` — Role `REQUESTER` + Own(recurso) → `200`

```json
{
  "requestId": "<uuid>",
  "newGoalBloodBags": 5
}
```

`newGoalBloodBags` > 0.

Response: `{ "id": "<uuid>", "goalBloodBags": "5" }`  
(`goalBloodBags` vem como **string**, não number)

---

### 5.5 Donations

#### `POST /donations/create-pending` — Role `DONOR` + Own(`personId`) → `201`

```json
{
  "organizationId": "<uuid hemocentro>",
  "personId": "<jwt partyId>",
  "expectedDate": "2026-09-01"
}
```

Response:

```json
{
  "id": "<uuid>",
  "expectedDate": "2026-09-01",
  "status": "PENDING"
}
```

#### `POST /donations/completed` — Role `DONOR` + Own(`personId`) → `201`

```json
{
  "personId": "<jwt partyId>",
  "organizationId": "<uuid>",
  "donationDate": "2026-08-01"
}
```

Response:

```json
{
  "id": "<uuid>",
  "donationDate": "2026-08-01",
  "status": "COMPLETED"
}
```

#### `PATCH /donations/complete` — Role `DONOR` + Own(doação) → `200`

```json
{
  "donationId": "<uuid>",
  "completionDate": "2026-08-01"
}
```

Response:

```json
{
  "id": "<uuid>",
  "completionDate": "2026-08-01",
  "status": "COMPLETED"
}
```

#### `PATCH /donations/reschedule` — Role `DONOR` + Own(doação) → `200`

```json
{
  "donationId": "<uuid>",
  "newExpectedDate": "2026-09-15"
}
```

Response:

```json
{
  "id": "<uuid>",
  "expectedDate": "2026-09-15",
  "status": "PENDING"
}
```

---

## 6. Matriz rápida path × auth

| Método | Path | Auth |
|--------|------|------|
| POST | `/auth/login` | Public |
| POST | `/parties/persons` | Public |
| POST | `/parties/organizations` | Public |
| PATCH | `/parties/name` | Auth + Own |
| POST | `/donors` | Auth + Own → re-login |
| PATCH | `/donors/profile` | Auth + Own |
| PATCH | `/donors/recommendation-distance` | DONOR + Own |
| GET | `/donors/{personId}/summary` | Auth + Own |
| GET | `/donors/{personId}/donations` | Auth + Own |
| POST | `/requesters` | Auth + Own → re-login |
| POST | `/blood-centers` | Auth + Own → re-login |
| POST | `/donation-requests` | REQUESTER + Own |
| GET | `/donation-requests/recommendations` | DONOR + Own |
| GET | `/donation-requests/{partyId}` | REQUESTER + Own |
| DELETE | `/donation-requests/{requestId}` | REQUESTER + Own(recurso) |
| POST | `/donation-requests/{id}/notify` | REQUESTER + Own(recurso) |
| PATCH | `/donation-requests/date-limit` | REQUESTER + Own(recurso) |
| PATCH | `/donation-requests/goal-blood-bags` | REQUESTER + Own(recurso) |
| POST | `/donations/create-pending` | DONOR + Own |
| POST | `/donations/completed` | DONOR + Own |
| PATCH | `/donations/complete` | DONOR + Own(recurso) |
| PATCH | `/donations/reschedule` | DONOR + Own(recurso) |

---

## 7. Checklist de alinhamento do frontend

Use como lista de trabalho no outro projeto:

- [ ] Cliente HTTP com `baseURL` sem `/api`
- [ ] Interceptor: `Authorization: Bearer <token>`
- [ ] Store/sessão: `accessToken`, `partyId`, `roles`, expiração
- [ ] Logout / redirect em `401`
- [ ] Mensagens amigáveis para `403` / `404` / `409` / `400` via `error`
- [ ] Após registro de papel → forçar novo login (ou chamar `/auth/login` de novo)
- [ ] Substituir qualquer `/requests/...` por `/donation-requests/...`
- [ ] Recommendations: query `personId` = usuário logado
- [ ] Criar pedido: `partyId` = usuário logado; `organizationId` = hemocentro escolhido
- [ ] Guards de UI por role (`DONOR` / `REQUESTER`) alinhados à matriz
- [ ] Forms de PATCH (perfil, distance, date-limit, goal, complete, reschedule) com campos exatos
- [ ] Tipagem: alguns campos de resposta de update são **string** (`goalBloodBags`, várias datas)
- [ ] Origem CORS / Vite `5173` se desenvolvimento local
- [ ] Remover chamadas a endpoints inexistentes neste contrato

---

## 8. Breaking changes conhecidos (em relação a clientes antigos)

1. `GET /requests/recommendations` → `GET /donation-requests/recommendations`
2. `POST /requests/{id}/notify` → `POST /donation-requests/{id}/notify`
3. Ownership reforçado: ids de outra party → `403` (antes muitos endpoints aceitavam qualquer id)
4. `PATCH /donations/reschedule` agora exige role `DONOR`
5. `POST /donation-requests/*/notify` agora exige role `REQUESTER`
6. CORS passa a permitir `PATCH` explicitamente

---

## 9. O que o frontend NÃO deve assumir

- Prefixo `/api` ou `/v1`
- Refresh token / cookie session
- Obter `SYSTEM_ADMIN` por UI pública
- Que `BLOOD_CENTER` liberará um conjunto grande de rotas (ainda não há superfície gated por esse role)
- Que registrar donor/requester atualiza o JWT automaticamente
- Que `goalBloodBags` no PATCH de meta volta como number (volta string)

---

*Gerado a partir do backend BloodMatch. Se o backend mudar, atualize este arquivo antes de pedir alinhamento no frontend.*
