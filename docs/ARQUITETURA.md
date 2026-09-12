# Arquitetura do BloodMatch

Fonte da verdade das camadas e do domínio. Inventário HTTP detalhado: [`FRONTEND_API_CONTRACT.md`](FRONTEND_API_CONTRACT.md). Snapshot de metas: [`LOGICA_PREENCHIMENTO_REQUESTS.md`](LOGICA_PREENCHIMENTO_REQUESTS.md).

## 1. Visão geral

API Spring Boot (Java 17) + MongoDB, com frontend React/Vite no mesmo monorepo. A API não usa prefixo `/api` nem versionamento de URL.

Divisão inspirada em DDD:

| Camada | Pacote | Responsabilidade |
|---|---|---|
| Domínio | `bloodmatch.domain` | Entidades, value objects, políticas e serviços de domínio |
| Aplicação | `bloodmatch.application.usecase` | Orquestração de casos de uso |
| REST | `bloodmatch.interfaces.rest` | Controllers e DTOs |
| Persistência | `bloodmatch.infra.persistence` | Repositórios e schemas MongoDB |
| Segurança | `bloodmatch.infra.security` | JWT (filtro + token) |
| Config | `bloodmatch.infra.config` | Security, CORS, OpenAPI, JWT properties, async |
| Externo | `bloodmatch.infra.external` | E-mail, geocoding (Google Maps), notificações |

Fluxo padrão:

```
interfaces/rest → application/usecase → domain → infra/persistence
```

O domínio não depende de HTTP nem de Mongo. Repositórios são interfaces no domínio, implementadas em `infra/persistence`.

## 2. Estrutura do monorepo

- `backend/` — API Spring Boot
- `frontend/` — app React + Vite + TypeScript
- `infrastructure/` — Nginx (produção)
- `docs/` — contratos e estratégias
- `scripts/` — seed de desenvolvimento
- `insomnia/` — coleção da API
- `tcc-latex/` — monografia

### Pacotes de domínio relevantes

- `domain/party` — Person, Organization
- `domain/roles` — Donor, Requester, BloodCenter
- `domain/donation` e `domain/donationrequest` — agregados
- `domain/matching` — `DonorMatchingService`
- `domain/services` — fulfillment, geocoding, notificação
- `domain/bloodcenter/inventory` e `domain/bloodcenter/schedule` — estoque e agenda
- `domain/security` — `UserAccount`, `SecurityRole`

## 3. Como executar

Pré-requisitos: Java 17, Maven, MongoDB, Node.js 22+ — ou Docker Compose.

```bash
docker compose up --build

cd backend && ./mvnw spring-boot:run
cd frontend && npm install && npm run dev

./scripts/seed-dev.sh
```

- Frontend: http://localhost:5173
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

## 4. Autenticação e papéis

JWT Bearer, stateless. Roles **sem** prefixo `ROLE_`:

| Role | Uso |
|---|---|
| `DONOR` | Doador |
| `REQUESTER` | Solicitante |
| `BLOOD_CENTER` | Hemocentro (estoque, agenda, consultas) |
| `SYSTEM_ADMIN` | Bypass de ownership; sem fluxo público |

Ownership: `partyId` / `personId` / `organizationId` da própria conta devem coincidir com o JWT (`requireSamePartyOrAdmin`). Violação → `403`.

Cadastro (`POST /parties/persons` ou `/parties/organizations`) pode exigir confirmação de e-mail (`app.require-email-confirmation`). Login com e-mail pendente → `401` `"Email not confirmed"`. Registro de papel **não** emite JWT novo: é preciso login de novo.

## 5. Endpoints atuais

### 5.1 Públicos

| Método | Caminho | Objetivo |
|---|---|---|
| POST | `/auth/login` | Autenticação JWT |
| GET/POST | `/auth/confirm-email` | Confirma e-mail (query `token` ou body) |
| POST | `/auth/resend-confirmation` | Reenvia e-mail de confirmação |
| POST | `/parties/persons` | Cadastro de pessoa |
| POST | `/parties/organizations` | Cadastro de organização |

### 5.2 Autenticados (conta e papéis)

| Método | Caminho | Objetivo |
|---|---|---|
| PATCH | `/parties` | Atualiza party (nome, telefone, endereço) |
| POST | `/donors` | Registra papel de doador |
| PATCH | `/donors` | Atualiza perfil do doador e preferências |
| GET | `/donors/{personId}/summary` | Resumo do doador |
| GET | `/donors/{personId}/donations` | Histórico de doações |
| POST | `/requesters` | Registra papel de solicitante |
| POST | `/blood-centers` | Registra papel de hemocentro |
| GET | `/blood-centers/search` | Busca hemocentro por nome |

### 5.3 Hemocentro (estoque, agenda, consultas)

| Método | Caminho | Objetivo |
|---|---|---|
| GET | `/blood-centers/inventory` | Visão geral de estoques |
| GET | `/blood-centers/{organizationId}/inventory` | Estoque de um hemocentro |
| PUT | `/blood-centers/inventory` | Publica estoque (própria org) |
| GET | `/blood-centers/appointments` | Doações pendentes do hemocentro |
| GET/PUT | `/blood-centers/schedule` | Agenda semanal da própria org |
| GET | `/blood-centers/{organizationId}/slots` | Slots disponíveis em uma data |

### 5.4 Donation requests

| Método | Caminho | Objetivo |
|---|---|---|
| POST | `/donation-requests` | Cria pedido |
| GET | `/donation-requests/{partyId}` | Lista pedidos do solicitante |
| PATCH | `/donation-requests` | Atualiza pedido (meta, prazo) |
| DELETE | `/donation-requests/{requestId}?version={version}` | Cancela pedido |
| GET | `/donation-requests/recommendations` | Recomenda pedidos ao doador |
| POST | `/donation-requests/{id}/notify` | Notifica doadores potenciais |

### 5.5 Donations

Um único create: `intendedDate` agenda (`PENDING`); `donationDate` registra já concluída (`COMPLETED`). Os dois campos são mutuamente exclusivos. `expectedTime` (`HH:mm`) é opcional ao agendar em slot.

| Método | Caminho | Objetivo |
|---|---|---|
| POST | `/donations` | Cria pendente ou concluída |
| PATCH | `/donations/complete` | Conclui doação pendente |
| PATCH | `/donations/reschedule` | Reagenda doação pendente |

**Nota sobre Concorrência (Optimistic Locking)**: Todos os endpoints de `PATCH` e `DELETE` (que alteram estado) recebem obrigatoriamente a propriedade `version` no corpo (ou query) para garantir consistência em ambientes concorrentes, lançando 409 Conflict em caso de versão divergente.

## 6. Regras de domínio importantes

- `Donor.isEligibleToDonate(currentDate)` — faixa etária e intervalo desde a última doação
- `DonationRequest.canBeFulfilledBy(...)` — request ativa, não expirada, compatibilidade ABO/Rh
- `DonationRequest.acceptsDonation(...)` — mesma org, janela `dateRequested ≤ donationDate ≤ dateLimit`, doação `COMPLETED`, tipo compatível
- `DonorMatchingService` — cruza elegibilidade do doador com requests ativas (notificação)

## 7. Fulfillment de requests (snapshot)

`DonationRequest` persiste só a meta (`goalBloodBags`). `fulfilledBloodBags` / `goalReached` **não** vão para o Mongo: são snapshot em memória do pool de doações `COMPLETED` do hemocentro.

`DonationRequestFulfillmentService`:

- `fill(bloodCenter, asOfDate)` — pedidos **ativos** daquele hemocentro
- `fill(requests, asOfDate)` — recarrega ativos dos hemocentros da lista (recomendações e listagem do requisitante)

O intervalo de doações começa na solicitação ativa mais antiga e vai até `asOfDate`. Duas passagens em memória, nada é regravado:

1. FIFO até o **teto proporcional** do dia (`proportionalGoalAt(asOfDate)`)
2. sobras em FIFO de novo, até a meta cheia (`goalBloodBags`)

O teto cresce com o prazo: `goalBloodBags × decorrido / janela` (calculado em `BigDecimal`). Com pool abundante o resultado coincide com FIFO puro; em escassez privilegiamos quem está perto de expirar. O progresso de uma request folgada pode **regredir** entre snapshots quando bolsas migram para uma irmã mais urgente.

Usado em:

- `GetDonationRequestsByPartyIdUseCase`
- `GetRecommendedRequestsUseCase` (omite meta já atingida)
- `NotifyPotentialDonorsUseCase` (bloqueia se a meta já foi atingida)

Criar ou completar doação só persiste a doação. O próximo GET monta o snapshot de novo.

Detalhe e exemplo: [`LOGICA_PREENCHIMENTO_REQUESTS.md`](LOGICA_PREENCHIMENTO_REQUESTS.md).

## 8. Persistência e mapeamento

- DTO → Domain: controllers / casos de uso
- Domain → Schema: construtores de schema
- Schema → Domain: `toDomain(...)`

Isso isola regras no domínio e deixa Mongo/REST nas bordas.

## 9. Artefatos de apoio

- [`FRONTEND_API_CONTRACT.md`](FRONTEND_API_CONTRACT.md) — payloads, auth e matriz path × role
- [`LOGICA_PREENCHIMENTO_REQUESTS.md`](LOGICA_PREENCHIMENTO_REQUESTS.md) — FIFO + teto proporcional
- [`FULFILLMENT_STRATEGY.md`](FULFILLMENT_STRATEGY.md) — resumo do snapshot
- `scripts/seed-dev.sh` — dados de desenvolvimento
- `insomnia/bloodmatch-jwt-e2e-insomnia-export.json` — coleção da API
- `frontend/ARCHITECTURE.md` — pastas e fluxos do app
