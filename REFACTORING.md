# BloodMatch — Refatoração Clean Architecture (camada REST)

Documento de referência sobre a refatoração que isolou controllers do domínio, padronizou DTOs de resposta e centralizou exceções tipadas.

---

## 1. Objetivo

Antes:

- Controllers construíam tipos de domínio (`DomainID`, `BloodType`, `Urgency`)
- Use cases devolviam entidades de domínio
- Respostas HTTP eram `Map.of(...)` ad-hoc
- Cada controller tinha `try/catch` local com `IllegalArgumentException` / `IllegalStateException`

Depois:

- Controllers só conhecem DTOs de request/response e `Input`/`Output` da aplicação
- Use cases fazem o parsing de value objects e mapeiam domínio → `Output`
- Respostas tipadas com `*ResponseDto`
- Erros HTTP centralizados via `@RestControllerAdvice`

Contrato JSON e status codes de sucesso foram preservados.

---

## 2. Linha de processamento atual

```text
HTTP Request
    │
    ▼
Controller (interfaces.rest)
    │  • valida payload (requireNotBlank / requireNonNull)
    │  • monta UseCase.Input (Strings / LocalDate / primitivos)
    │  • NÃO importa bloodmatch.domain.*
    ▼
Use Case (application.usecase)
    │  • DomainIdParser / BloodType.of / Urgency.valueOf
    │  • orquestra repositórios e regras de domínio
    │  • lança ValidationException | NotFoundException | ConflictException | …
    │  • devolve Output (sem entidades de domínio no contrato público)
    ▼
ResponseDto.from(output)
    │
    ▼
HTTP Response (JSON tipado)
```

Em caso de erro:

```text
Controller / Use Case / Domínio
    │  throw ApplicationException (ou Illegal* no domínio)
    ▼
RestExceptionHandler (@RestControllerAdvice)
    │
    ▼
ErrorResponseDto { "error": "mensagem" } + status HTTP
```

### Exemplo concreto — criar doação pendente

1. `POST /donations/create-pending` → `CreatePendingDonationController`
2. Valida `organizationId`, `personId`, `expectedDate`
3. `useCase.execute(new Input(...))`
4. Use case parseia UUIDs, busca `Donor` / `BloodCenter`, cria `Donation`, persiste
5. Retorna `Output(id, expectedDate, status)`
6. Controller responde `201` + `CreatePendingDonationResponseDto`

Arquivos de referência:

- [`CreatePendingDonationController.java`](src/main/java/bloodmatch/interfaces/rest/donation/createpending/CreatePendingDonationController.java)
- [`CreatePendingDonationUseCase.java`](src/main/java/bloodmatch/application/usecase/donation/createpending/CreatePendingDonationUseCase.java)

---

## 3. Camadas e responsabilidades

| Camada | Pacote | Responsabilidade |
|--------|--------|------------------|
| Interface (REST) | `bloodmatch.interfaces.rest.*` | HTTP, DTOs, validação de entrada, mapping Output → ResponseDto |
| Aplicação | `bloodmatch.application.usecase.*` | Casos de uso, Input/Output, orquestração |
| Aplicação (erros) | `bloodmatch.application.exception.*` | Exceções tipadas de aplicação |
| Domínio | `bloodmatch.domain.*` | Entidades, VOs, regras, interfaces de repositório |
| Infra | `bloodmatch.infra.*` | Mongo, JWT, e-mail, Google Maps, SecurityConfig |

### Shared útil

| Tipo | Onde | Função |
|------|------|--------|
| `DomainIdParser` | `application.shared` | String → `DomainID` (lança `ValidationException`) |
| `RequestValidationSupport` | `interfaces.rest.shared` | `requireNotBlank` / `requireNonNull` / `isBlank` |
| `AuthenticatedPartySupport` | `interfaces.rest.shared` | Ownership: mesmo `partyId` do JWT ou `SYSTEM_ADMIN` |
| `ErrorResponseDto` | `interfaces.rest.shared` | Corpo padrão de erro |
| `RestExceptionHandler` | `interfaces.rest.shared` | Mapeamento exceção → HTTP |

---

## 4. Exceções tipadas

Hierarquia: `ApplicationException` (base)

| Exceção | HTTP | Quando usar |
|---------|------|-------------|
| `ValidationException` | 400 | Input inválido / campo obrigatório |
| `NotFoundException` | 404 | Entidade/role não encontrada |
| `ConflictException` | 409 | Já registrado / estado conflitante |
| `ForbiddenException` | 403 | Acesso a recurso de outro party |
| `UnauthorizedException` | 401 | Credenciais inválidas (login) |

Fallback: `IllegalArgumentException` / `IllegalStateException` (ainda lançadas pelo domínio) → **400**.

Corpo de erro (inalterado em formato):

```json
{ "error": "mensagem descritiva" }
```

Controllers **não** usam mais `try/catch` local.

---

## 5. Padrão de código (convenção)

### Use case

```java
public Output execute(Input input) { ... }

public record Input(String personId, String organizationId, LocalDate expectedDate) {}
public record Output(String id, LocalDate expectedDate, String status) {
  public static Output from(Donation donation) { ... }
}
```

- Signature pública **sem** tipos de `bloodmatch.domain.*`
- Enums de domínio no fio viram `String` (ex.: `urgency.name()`, status `"PENDING"`)

### Controller

```java
requireNotBlank(payload.personId(), "personId cannot be blank");
var output = useCase.execute(new Input(...));
return ResponseEntity.status(CREATED).body(ResponseDto.from(output));
```

### Response DTO

- Nome: `*ResponseDto`
- Local: mesmo pacote do controller
- Mapping: `static from(UseCase.Output)`

---

## 6. Endpoints cobertos

Todos os controllers REST passaram pelo padrão (sem imports de domínio):

- Auth: login
- Party: register person/organization, update name
- Role: register donor/requester/blood-center, update donor profile/distance, donor summary
- Donation: create pending/completed, complete, reschedule, history
- Donation request: create, list by party, update date/goal, cancel, recommendations, notify donors

Ownership explícito:

- `GET /donation-requests/{partyId}` → `AuthenticatedPartySupport.requireSamePartyOrAdmin(partyId)`

---

## 7. O que NÃO mudou

- URLs e campos JSON de sucesso
- Validação manual (sem Bean Validation `@Valid` ainda)
- Deploy Heroku / estrutura de domínio interno
- Domínio ainda pode lançar `Illegal*` (coberto pelo fallback do handler)

---

## 8. Runtime local — pontos de atenção

A API importa `.env` via:

```properties
spring.config.import=optional:file:.env[.properties]
```

Variáveis necessárias (ver `.env.example`):

| Variável | Uso |
|----------|-----|
| `MONGODB_URI` / `MONGODB_DATABASE` | Persistência |
| `JWT_SECRET` (+ expiration/issuer) | Auth |
| `GOOGLE_MAPS_API_KEY` | Geocoding |
| `GMAIL_USERNAME` / `GMAIL_PASSWORD` | SMTP (bean `emailNotificationService`) |
| `PORT` | Server (default 8080) |

Falha comum ao subir: placeholder `GMAIL_USERNAME` não resolvido se faltar no `.env` — a app não inicia porque `EmailNotificationService` depende de `spring.mail.username`.

Testes usam overrides em `src/test/resources/application.properties` (JWT de teste + mail localhost).

---

## 9. Como evoluir a partir daqui

Sugestões naturais (fora do escopo já feito):

1. Tornar e-mail opcional (`@ConditionalOnProperty`) para local sem Gmail
2. Migrar gradualmente o domínio de `Illegal*` para exceções tipadas
3. Introduzir Bean Validation nos request DTOs
4. Docker Compose (Mongo + app) para ambiente virtualizado
5. Alinhar use cases sem controller (`FindEligibleDonors`, etc.) ao mesmo contrato Input/Output quando forem expostos

---

## 10. Checklist rápido para um novo endpoint

1. Criar/ajustar use case com `Input` + `Output` (só tipos simples)
2. Lançar exceções tipadas (`Validation` / `NotFound` / `Conflict` / …)
3. Criar request DTO (se body) + `*ResponseDto.from(Output)`
4. Controller: validar → `execute(Input)` → `ResponseEntity<ResponseDto>`
5. Zero imports de `bloodmatch.domain.*` no controller
6. Sem `try/catch` / sem `Map.of` de resposta
7. Cobrir com teste de use case assertando em `Output`
