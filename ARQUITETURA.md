# Arquitetura do Bloodmatch

## 1. Visao geral

O projeto segue uma divisao em camadas inspirada em DDD:

- domain: entidades, value objects e regras de negocio
- application/usecase: orquestracao de casos de uso
- interfaces/rest: controllers e DTOs
- infra/persistence: repositorios e schemas MongoDB
- infra/security: JWT e autorizacao

Fluxo padrao de chamada:

interfaces/rest -> application/usecase -> domain -> infra/persistence

## 2. Estrutura do projeto

Monorepo:

- `backend/` — API Spring Boot
- `frontend/` — app React/Vite
- `scripts/`, `insomnia/`, `docs/`, `tcc-latex/`

Camadas do backend:

- backend/src/main/java/bloodmatch/domain
- backend/src/main/java/bloodmatch/application/usecase
- backend/src/main/java/bloodmatch/interfaces/rest
- backend/src/main/java/bloodmatch/infra/persistence
- backend/src/main/java/bloodmatch/infra/security

## 3. Como executar localmente

Pre-requisitos:

- Java 17
- Maven
- MongoDB
- Node.js 22+ (frontend)
- ou Docker Compose

Comandos:

```bash
# stack completa
docker compose up --build

# backend isolado
cd backend && ./mvnw spring-boot:run

# frontend isolado
cd frontend && npm install && npm run dev

./scripts/seed-dev.sh
```

## 4. Endpoints atuais

### 4.1 Publicos

| Metodo | Caminho | Objetivo |
|---|---|---|
| POST | /auth/login | Autenticacao JWT |
| POST | /parties/persons | Cadastro de pessoa |
| POST | /parties/organizations | Cadastro de organizacao |

### 4.2 Autenticados

| Metodo | Caminho | Objetivo |
|---|---|---|
| PATCH | /parties/name | Atualiza nome da party |
| POST | /donors | Registra papel de doador |
| PATCH | /donors/profile | Atualiza perfil do doador |
| PATCH | /donors/recommendation-distance | Ajusta distancia maxima de recomendacao |
| GET | /donors/{personId}/summary | Resumo do doador |
| GET | /donors/{personId}/donations | Historico de doacoes |
| POST | /requesters | Registra papel de solicitante |
| POST | /blood-centers | Registra papel de hemocentro |

### 4.3 Donation requests

| Metodo | Caminho | Objetivo |
|---|---|---|
| POST | /donation-requests | Cria pedido de doacao |
| GET | /donation-requests/{partyId} | Lista pedidos por party |
| PATCH | /donation-requests/goal-blood-bags | Atualiza meta de bolsas |
| PATCH | /donation-requests/date-limit | Atualiza prazo |
| DELETE | /donation-requests/{requestId} | Cancela pedido |
| GET | /donation-requests/recommendations | Recomenda pedidos para doador |
| POST | /donation-requests/{id}/notify | Notifica doadores potenciais |

### 4.4 Donations

| Metodo | Caminho | Objetivo |
|---|---|---|
| POST | /donations/create-pending | Cria doacao pendente |
| POST | /donations/completed | Registra doacao concluida |
| PATCH | /donations/complete | Conclui doacao pendente |
| PATCH | /donations/reschedule | Reagenda doacao pendente |

## 5. Regras de dominio importantes

- Donor.isEligibleToDonate(currentDate)
  - valida faixa etaria e intervalo desde a ultima doacao
- DonationRequest.canBeFulfilledBy(...)
  - valida request ativa, nao expirada e compatibilidade sanguinea
- DonationRequest.acceptsDonation(...)
  - valida se uma doacao concluida pode ser alocada naquela request

## 6. Fulfillment de requests (estado persistido)

### 6.1 Modelo

DonationRequest possui estado de progresso persistido:

- goalBloodBags
- fulfilledBloodBags
- isGoalReached()

Esse estado é salvo em MongoDB no schema com versao otimista.

### 6.2 Servico

DonationRequestFulfillmentService:

- calculate(requests, donations, currentDate)
  - calcula alocacao FIFO por hemocentro e compatibilidade
- synchronize(requests, donations, currentDate)
  - aplica o resultado do calculo em fulfilledBloodBags de cada request

### 6.3 Quando o estado é recalculado

No write path de doacao concluida:

- CreateCompletedDonationUseCase
- CompletePendingDonationUseCase

Ambos delegam para `DonationRequestFulfillmentRefresher.refresh(organizationId, currentDate)`:

1. persistem a doacao (no use case)
2. carregam requests ativas da mesma organization
3. carregam doacoes concluidas da organization
4. executam synchronize
5. persistem requests atualizadas

### 6.4 Onde o estado é consumido

No read path:

- GetDonationRequestsByPartyIdUseCase
  - usa fulfilledBloodBags e isGoalReached ja persistidos
- GetRecommendedRequestsUseCase
  - filtra requests com meta ja atingida
- NotifyPotentialDonorsUseCase
  - bloqueia notificacao se request ja atingiu meta

## 7. Confiabilidade e concorrencia

- DonationRequestSchema usa controle de versao otimista (@Version)
- o repositorio converte OptimisticLockingFailureException para IllegalStateException
- o write path de doacao completa e serializado por organizationId (`OrganizationFulfillmentLock`)
- o refresher relê o lote e tenta de novo se o @Version colidir; esgotou retries → HTTP 409
- detalhes e o furo original: docs/FULFILLMENT_CONCURRENCY.md

## 8. Persistencia e mapeamento

Conversoes principais:

- DTO -> Domain: controllers
- Domain -> Schema: construtores de schema
- Schema -> Domain: toDomain(...)

Esse padrao isola regras de negocio no dominio e deixa persistencia/REST em camadas externas.

## 9. Artefatos de apoio

- scripts/seed-dev.sh: popula ambiente de desenvolvimento
- scripts/simulate-usage.py: simula uso concorrente e recorrente do fulfillment
- insomnia/bloodmatch-jwt-e2e-insomnia-export.json: colecao de chamadas da API
