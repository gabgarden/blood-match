# BloodMatch — Contrato da API (Frontend)

Documento de referência do **backend atual** (Spring Boot, JWT, sem prefixo `/api` e sem versionamento de URL).

Arquitetura e camadas: [`ARQUITETURA.md`](ARQUITETURA.md). Snapshot de metas: [`LOGICA_PREENCHIMENTO_REQUESTS.md`](LOGICA_PREENCHIMENTO_REQUESTS.md).

Não invente endpoints. Se o backend mudar, atualize este arquivo primeiro.

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
| Horários | `HH:mm` (ex.: `08:00`) |
| Docs interativas | `/swagger-ui.html`, `/v3/api-docs` |

### Header de autenticação

```http
Authorization: Bearer <accessToken>
```

### CORS (browser)

Origens/padrões incluem `http://localhost:*`, `http://127.0.0.1:*`, `https://gabgarden.github.io` e `bloodmatch.com.br` (com/sem `www`, http/https). Lista explícita via `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173,http://127.0.0.1:5173,https://gabgarden.github.io`).

Métodos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`  
Credentials: `true`

---

## 2. Auth, roles e ownership

### 2.1 Login

`POST /auth/login` — **público**

```json
{ "email": "user@email.com", "password": "senha" }
```

`200`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "roles": ["DONOR"],
  "partyId": "<uuid>"
}
```

Persistir: `accessToken`, `partyId`, `roles`, `expiresIn`.

E-mail ainda não confirmado → `401` `{ "error": "Email not confirmed" }`.

### 2.2 Confirmação de e-mail — **público**

Cadastro devolve `emailConfirmationRequired`. Se `true`, o usuário **não** deve tentar login até confirmar.

`GET /auth/confirm-email?token=` e `POST /auth/confirm-email` com `{ "token": "..." }` — mesmo efeito.

`200`: `{ "message": "Email confirmed", "email": "user@email.com" }`

`POST /auth/resend-confirmation` — `{ "email": "user@email.com" }`

`200`: mensagem genérica (não revela se a conta existe):  
`"If the email is registered and pending confirmation, a new message was sent."`

O link do e-mail aponta para o frontend: `/confirm-email?token=...`.

### 2.3 Roles (`SecurityRole`)

| Role | Significado |
|------|-------------|
| `DONOR` | Doador |
| `REQUESTER` | Solicitante |
| `BLOOD_CENTER` | Hemocentro (estoque, agenda, consultas, busca) |
| `SYSTEM_ADMIN` | Admin (bypass de ownership; sem fluxo público) |

Roles no JWT **não** usam prefixo `ROLE_`.

### 2.4 Fluxo típico de conta

1. Registrar pessoa/org → `{ "id", "type", "emailConfirmationRequired" }`.
2. Se confirmação for exigida: tela de check-email → `GET/POST /auth/confirm-email`.
3. Login → JWT com `partyId` e `roles` (vazios até registrar papel).
4. Registrar papel (`POST /donors`, `/requesters` ou `/blood-centers`).
5. **Login de novo** — o backend **não** emite token novo no registro de papel. Sem re-login → **403** nas rotas gated.
6. Não há refresh token.

Papéis pendentes no frontend (doador/hemocentro escolhidos no cadastro) devem ser aplicados **depois** do primeiro login com e-mail já confirmado.

### 2.5 Ownership (obrigatório)

1. `partyId` / `personId` / `organizationId` (quando são *a própria* conta) = `partyId` do JWT.
2. Operações por `requestId` / `donationId` só na própria conta (ou admin).
3. Violação → **403** `{ "error": "Forbidden" }`.

`PUT /blood-centers/inventory` e `PUT /blood-centers/schedule` usam a org autenticada; o body **não** envia `organizationId`.

---

## 3. Erros

```json
{ "error": "<mensagem>" }
```

| Status | Quando |
|--------|--------|
| `400` | Validação / regra de negócio inválida |
| `401` | Sem token, token inválido/expirado, credenciais erradas, e-mail não confirmado |
| `403` | Role insuficiente ou ownership |
| `404` | Recurso não encontrado |
| `409` | Conflito (e-mail já cadastrado; papel já registrado) |

Mensagens comuns:

- `"Unauthorized"`
- `"Invalid or expired token"`
- `"Email not confirmed"`
- `"Invalid credentials"`
- `"Forbidden"`
- `"Email already registered"`
- `"Donor already registered for person"` (e equivalentes de requester/blood center)

Tratar `401` (logout / login) e `403` (permissão / papel incompleto).

---

## 4. Enums

### Blood type

`A+` | `A-` | `B+` | `B-` | `AB+` | `AB-` | `O+` | `O-`

### Urgency

`LOW` | `MEDIUM` | `CRITICAL`

### Donation status

`PENDING` | `COMPLETED` | `CANCELLED`

### Inventory label (resposta)

`Crítico` (`< 30%`) | `Alerta` (`≤ 70%`) | `Adequado`

### Weekday (agenda)

`MONDAY` … `SUNDAY`

### Party type

`"PERSON"` | `"ORGANIZATION"`

---

## 5. Inventário de endpoints

Legenda: **Public** · **Auth** (qualquer JWT) · **Role** · **Own** (própria party / recurso)

### 5.1 Auth

#### `POST /auth/login` — Public → `200`

Body: `email`, `password`.

#### `GET /auth/confirm-email?token=` — Public → `200`

#### `POST /auth/confirm-email` — Public → `200`

Body: `{ "token": "..." }`

#### `POST /auth/resend-confirmation` — Public → `200`

Body: `{ "email": "..." }`

---

### 5.2 Parties

#### `POST /parties/persons` — Public → `201`

| Campo | Tipo | Obrigatório |
|-------|------|-------------|
| `name` | string | sim |
| `phoneNumber` | string | sim |
| `cpf` | string | sim |
| `birthDate` | date | sim |
| `email` | string | sim |
| `password` | string | sim |
| `passwordConfirmation` | string | sim |
| `street`, `city`, `state`, `zipCode` | string | não* |

\* Se qualquer campo de endereço for enviado, **os quatro** devem vir preenchidos.

Response: `{ "id": "<uuid>", "type": "PERSON", "emailConfirmationRequired": true }`

#### `POST /parties/organizations` — Public → `201`

Body: `name`, `phoneNumber`, `cnpj`, `email`, `password`, `passwordConfirmation`, endereço opcional (mesma regra).

Response: `{ "id": "<uuid>", "type": "ORGANIZATION", "emailConfirmationRequired": true }`

#### `PATCH /parties` — Auth + Own(`partyId`) → `200`

```json
{ 
  "partyId": "<uuid>", 
  "version": 1,
  "name": "Novo nome",
  "phoneNumber": "11999999999",
  "street": "Rua X",
  "city": "São Paulo",
  "state": "SP",
  "zipCode": "01000-000"
}
```

(Propriedades de update são opcionais, exceto `partyId` e `version`)
Response: `{ "id": "<uuid>", "version": 2, "name": "Novo nome" }`

---

### 5.3 Papéis / doador / busca de hemocentro

#### `POST /donors` — Auth + Own(`personId`) → `201`

```json
{
  "personId": "<partyId do JWT>",
  "bloodType": "O+",
  "weight": 70.5,
  "lastDonationDate": "2026-01-15"
}
```

`lastDonationDate` é opcional e não pode ser futura.

Response: `{ "id": "<donorRoleId>" }`  
**Depois: re-login.**

#### `POST /requesters` — Auth + Own(`partyId`) → `201`

```json
{ "partyId": "<partyId do JWT>" }
```

**Depois: re-login.**

#### `POST /blood-centers` — Auth + Own(`organizationId`) → `201`

```json
{ "organizationId": "<partyId do JWT>" }
```

**Depois: re-login.**

#### `GET /blood-centers/search?q=&limit=` — Roles `DONOR` \| `REQUESTER` \| `BLOOD_CENTER` \| `SYSTEM_ADMIN` → `200`

| Query | Obrigatório | Regras |
|-------|-------------|--------|
| `q` | sim | ≥ 2 caracteres |
| `limit` | não | default `10`, máximo `20` |

```json
[
  {
    "organizationId": "<uuid>",
    "name": "Hemocentro Regional de Campos",
    "city": "Campos dos Goytacazes",
    "state": "RJ"
  }
]
```

Use `organizationId` em `POST /donation-requests` e `POST /donations`.

#### `PATCH /donors` — Role `DONOR` + Own(`personId`) → `200`

```json
{ 
  "personId": "<uuid>", 
  "version": 1,
  "bloodType": "A+", 
  "weight": 72.0,
  "maxDistanceInKm": 30 
}
```

Response: `{ "id": "<uuid>", "version": 2 }`

#### `GET /donors/{personId}/summary` — Auth + Own(path) → `200`

```json
{
  "personId": "<uuid>",
  "partyVersion": 1,
  "donorVersion": 1,
  "donorName": "string",
  "phoneNumber": "string",
  "bloodType": "O+",
  "address": "string",
  "lastDonationDate": "2026-01-15",
  "daysRemaining": 0,
  "livesImpacted": 0,
  "weight": 70.5,
  "weightUpdatedAt": "2026-01-15"
}
```

`lastDonationDate` e `weight` / `weightUpdatedAt` podem ser `null`.

#### `GET /donors/{personId}/donations` — Auth + Own(path) → `200`

`status` é `PENDING` | `COMPLETED` | `CANCELLED`. `date` é a data de referência (conclusão ou data pretendida).

```json
[{ "donationId": "<uuid>", "version": 1, "date": "2026-01-15", "location": "Nome do hemocentro", "status": "COMPLETED" }]
```

---

### 5.4 Hemocentro — estoque, agenda e consultas

#### `GET /blood-centers/inventory` — Roles `DONOR` \| `REQUESTER` \| `BLOOD_CENTER` \| `SYSTEM_ADMIN` → `200`

Lista de hemocentros com níveis:

```json
[{
  "organizationId": "<uuid>",
  "name": "string",
  "city": "string",
  "state": "string",
  "updatedAt": "2026-08-23T10:00:00",
  "items": [{ "bloodType": "O+", "percentage": 40, "label": "Alerta" }]
}]
```

#### `GET /blood-centers/{organizationId}/inventory` — mesmas roles → `200`

```json
{
  "organizationId": "<uuid>",
  "updatedAt": "...",
  "items": [{ "bloodType": "O+", "percentage": 40, "label": "Alerta" }]
}
```

#### `PUT /blood-centers/inventory` — Role `BLOOD_CENTER` + Own(JWT) → `200`

A org é a do token. Body:

```json
{ "items": [{ "bloodType": "O+", "percentage": 40 }] }
```

Tipos omitidos entram como `0`. Percentual é limitado a `0..100`.

#### `GET /blood-centers/appointments?from=&to=` — Role `BLOOD_CENTER` + Own → `200`

`from` / `to` opcionais (`YYYY-MM-DD`). Lista doações **pendentes** daquela org.

```json
[{
  "donationId": "<uuid>",
  "expectedDate": "2026-09-01",
  "expectedTime": "08:00",
  "status": "PENDING",
  "donorName": "string",
  "donorBloodType": "O+",
  "donorPhone": "string"
}]
```

#### `GET /blood-centers/schedule` — Role `BLOOD_CENTER` + Own → `200`

#### `PUT /blood-centers/schedule` — Role `BLOOD_CENTER` + Own → `200`

```json
{
  "weeklyWindows": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "08:00",
      "endTime": "12:00",
      "slotDurationMinutes": 30,
      "capacity": 4
    }
  ],
  "blockedDates": ["2026-12-25"]
}
```

Response: `organizationId`, `weeklyWindows`, `blockedDates`.

#### `GET /blood-centers/{organizationId}/slots?date=` — Roles `DONOR` \| `REQUESTER` \| `BLOOD_CENTER` \| `SYSTEM_ADMIN` → `200`

Query `date` obrigatória.

```json
{
  "organizationId": "<uuid>",
  "date": "2026-09-01",
  "hasSchedule": true,
  "slots": [{
    "startTime": "08:00",
    "endTime": "08:30",
    "capacity": 4,
    "booked": 1,
    "available": 3
  }]
}
```

Se o hemocentro tem slots na data, `POST /donations` com `intendedDate` deve enviar `expectedTime` igual a um `startTime` disponível.

---

### 5.5 Donation requests

Paths antigos `/requests/...` foram unificados em `/donation-requests/...`.

#### `POST /donation-requests` — Role `REQUESTER` + Own(`partyId`) → `201`

```json
{
  "partyId": "<requester = JWT>",
  "organizationId": "<uuid do hemocentro>",
  "bloodTypeNeeded": "A+",
  "goalBloodBags": 3,
  "dateLimit": "2026-12-31",
  "urgency": "MEDIUM",
  "directedTo": "opcional"
}
```

`goalBloodBags` > 0. `organizationId` é o hemocentro destino (não é o JWT).

Response: `{ "id": "<requestId>" }`

#### `GET /donation-requests/recommendations?personId=&includeNonEligible=` — Role `DONOR` + Own(query) → `200`

`personId` obrigatório (= JWT). `includeNonEligible` default `false`.

```json
[{
  "requestId": "<uuid>",
  "organizationId": "<uuid>",
  "bloodTypeNeeded": "A+",
  "dateLimit": "2026-12-31",
  "bloodCenterName": "string",
  "urgency": "MEDIUM",
  "distanceInKm": 12.5,
  "goalBloodBags": 3,
  "fulfilledBloodBags": 1,
  "goalReached": false,
  "latitude": -21.75,
  "longitude": -41.33
}]
```

`organizationId` (e `expectedTime` via slots) alimentam `POST /donations`. Pedidos com meta já atingida no snapshot **não** entram na lista.

#### `GET /donation-requests/{partyId}` — Role `REQUESTER` + Own(path) → `200`

Não use o literal `recommendations` como `{partyId}`.

`fulfilledBloodBags` / `remainingBloodBags` / `goalReached` vêm do snapshot em memória, não de campo persistido.

```json
[{
  "requestId": "<uuid>",
  "version": 1,
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
}]
```

Na listagem, `goalBloodBags` é **number**. No PATCH, a resposta atualizada é devolvida.

#### `DELETE /donation-requests/{requestId}?version={version}` — Role `REQUESTER` + Own(recurso) → `204`

#### `POST /donation-requests/{id}/notify` — Role `REQUESTER` + Own(recurso) → `200`

Sem body. Bloqueia se a meta do snapshot já foi atingida.

Response: `{ "message": "Notifications sent to eligible donors successfully." }`

#### `PATCH /donation-requests` — Role `REQUESTER` + Own(recurso) → `200`

```json
{ 
  "requestId": "<uuid>", 
  "version": 1,
  "dateLimit": "2026-12-31",
  "goalBloodBags": 5 
}
```

As propriedades de update são opcionais, exceto `requestId` e `version`. `dateLimit` não pode ser no passado. `goalBloodBags` > 0.
Response: `{ "id": "<uuid>", "version": 2, "dateLimit": "2026-12-31", "goalBloodBags": "5" }`

---

### 5.6 Donations

#### `POST /donations` — Role `DONOR` + Own(`personId`) → `201`

Um endpoint para os dois casos. Envie **só um** de `intendedDate` ou `donationDate`.

Agendar:

```json
{
  "personId": "<jwt partyId>",
  "organizationId": "<uuid hemocentro>",
  "intendedDate": "2026-09-01",
  "expectedTime": "08:00"
}
```

Registrar já concluída:

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
  "intendedDate": "2026-09-01",
  "donationDate": null,
  "expectedTime": "08:00",
  "status": "PENDING"
}
```

Não use `POST /donations/create-pending` nem `POST /donations/completed` — foram removidos.

#### `PATCH /donations/complete` — Role `DONOR` + Own(doação) → `200`

```json
{ "donationId": "<uuid>", "version": 1, "completionDate": "2026-08-01" }
```

Response: `{ "id": "<uuid>", "version": 2, "completionDate": "2026-08-01", "status": "COMPLETED" }` (`completionDate` string).

#### `PATCH /donations/reschedule` — Role `DONOR` + Own(doação) → `200`

```json
{ "donationId": "<uuid>", "version": 1, "newExpectedDate": "2026-09-15" }
```

Response: `{ "id": "<uuid>", "version": 2, "expectedDate": "2026-09-15", "status": "PENDING" }` (`expectedDate` string).

---

## 6. Matriz path × auth

| Método | Path | Auth |
|--------|------|------|
| POST | `/auth/login` | Public |
| GET/POST | `/auth/confirm-email` | Public |
| POST | `/auth/resend-confirmation` | Public |
| POST | `/parties/persons` | Public |
| POST | `/parties/organizations` | Public |
| PATCH | `/parties` | Auth + Own |
| POST | `/donors` | Auth + Own → re-login |
| PATCH | `/donors` | Auth + Own |
| GET | `/donors/{personId}/summary` | Auth + Own |
| GET | `/donors/{personId}/donations` | Auth + Own |
| POST | `/requesters` | Auth + Own → re-login |
| POST | `/blood-centers` | Auth + Own → re-login |
| GET | `/blood-centers/search` | DONOR / REQUESTER / BLOOD_CENTER / SYSTEM_ADMIN |
| GET | `/blood-centers/inventory` | DONOR / REQUESTER / BLOOD_CENTER / SYSTEM_ADMIN |
| GET | `/blood-centers/{organizationId}/inventory` | DONOR / REQUESTER / BLOOD_CENTER / SYSTEM_ADMIN |
| PUT | `/blood-centers/inventory` | BLOOD_CENTER + Own |
| GET | `/blood-centers/appointments` | BLOOD_CENTER + Own |
| GET/PUT | `/blood-centers/schedule` | BLOOD_CENTER + Own |
| GET | `/blood-centers/{organizationId}/slots` | DONOR / REQUESTER / BLOOD_CENTER / SYSTEM_ADMIN |
| POST | `/donation-requests` | REQUESTER + Own |
| GET | `/donation-requests/recommendations` | DONOR + Own |
| GET | `/donation-requests/{partyId}` | REQUESTER + Own |
| DELETE | `/donation-requests/{requestId}?version={version}` | REQUESTER + Own(recurso) |
| POST | `/donation-requests/{id}/notify` | REQUESTER + Own(recurso) |
| PATCH | `/donation-requests` | REQUESTER + Own(recurso) |
| POST | `/donations` | DONOR + Own |
| PATCH | `/donations/complete` | DONOR + Own(recurso) |
| PATCH | `/donations/reschedule` | DONOR + Own(recurso) |

Swagger `/swagger-ui/**` e `/v3/api-docs/**` são públicos.

---

## 7. Checklist do frontend

- [ ] Cliente HTTP com `baseURL` sem `/api`
- [ ] Interceptor `Authorization: Bearer <token>`
- [ ] Sessão: `accessToken`, `partyId`, `roles`, expiração
- [ ] Cadastro: respeitar `emailConfirmationRequired`; confirmar / reenviar e-mail
- [ ] Login: tratar `"Email not confirmed"`
- [ ] Após registro de papel → novo login
- [ ] `POST /donations` (não os paths antigos de create-pending/completed)
- [ ] Recomendações: `personId` = usuário logado; usar `organizationId` (+ slots/`expectedTime`) para agendar
- [ ] Pedido: `partyId` = logado; `organizationId` = hemocentro buscado
- [ ] Guards por `DONOR` / `REQUESTER` / `BLOOD_CENTER`
- [ ] PATCH de meta: `goalBloodBags` na resposta é **string**
- [ ] Histórico do doador usa `status` (`PENDING` | `COMPLETED` | `CANCELLED`) para agendar / concluir / reagendar

---

## 8. Breaking changes (clientes antigos)

1. `/requests/...` → `/donation-requests/...`
2. `POST /donations/create-pending` e `POST /donations/completed` → `POST /donations`
3. Ownership: id de outra party → `403`
4. Confirmação de e-mail no cadastro/login
5. Superfície `BLOOD_CENTER`: estoque, agenda, slots, appointments
6. Recommendations incluem `organizationId`, `latitude`, `longitude`
7. `GET /donation-requests/recommendations` aceita `includeNonEligible`

---

## 9. O que o frontend NÃO deve assumir

- Prefixo `/api` ou `/v1`
- Refresh token / cookie de sessão
- Obter `SYSTEM_ADMIN` por UI pública
- Que registrar papel atualiza o JWT sozinho
- Que `goalBloodBags` no PATCH de meta volta como number
- Que progresso de bolsas está persistido no Mongo
