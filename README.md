# BloodMatch (monorepo)

Plataforma de matching entre doadores e solicitações de sangue.

```
blood-match/
├── backend/          # Spring Boot (Java 17) + MongoDB
├── frontend/         # React + Vite + TypeScript
├── infrastructure/     # Configurações de infraestrutura (Nginx)
├── docs/             # Contratos e estratégias
├── scripts/          # Seed e migrações
├── insomnia/         # Coleção de API
├── tcc-latex/        # TCC
├── docker-compose.yml
├── docker-compose.prod.yml
└── .env.example
```

## Pré-requisitos

- Docker + Docker Compose **ou**
- Java 17 + Maven e Node.js 22+

## Configuração

```bash
cp .env.example .env
# edite MONGODB_URI, JWT_SECRET, SPRING_MAIL_*, GOOGLE_MAPS_API_KEY
```

## Subir com Docker Compose (Desenvolvimento)

```bash
docker compose up --build
```

- Frontend (Vite): http://localhost:5173  
- Backend API: http://localhost:8080  
- Swagger: http://localhost:8080/swagger-ui.html  

O browser chama a API em `VITE_API_BASE_URL` (padrão `http://localhost:8080`).

### Build de produção (VPS / Hostinger)

```bash
cp .env.production.example .env.production
# edite JWT_SECRET e senhas no arquivo .env.production

docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.production up -d --build
```

- App / Nginx Proxy: http://localhost:8082  
- Backend API: http://localhost:8080  

## Desenvolvimento local (sem Docker)

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

O Spring carrega `.env` de `backend/.env` ou da raiz do monorepo (`../.env`).

### Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

## Seed de dados

Com a API no ar:

```bash
./scripts/seed-dev.sh
```

## Documentação

| Arquivo | Conteúdo |
|---|---|
| [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) | Camadas DDD, pacotes e mapa da API |
| [`docs/FRONTEND_API_CONTRACT.md`](docs/FRONTEND_API_CONTRACT.md) | Contrato HTTP (paths, payloads, roles) |
| [`docs/LOGICA_PREENCHIMENTO_REQUESTS.md`](docs/LOGICA_PREENCHIMENTO_REQUESTS.md) | Snapshot FIFO das metas |
| [`docs/FULFILLMENT_STRATEGY.md`](docs/FULFILLMENT_STRATEGY.md) | Resumo do snapshot |
| [`frontend/ARCHITECTURE.md`](frontend/ARCHITECTURE.md) | Pastas e fluxos do app |
| [`ACESSOS.md`](ACESSOS.md) | URLs, portas e logins de seed |
| [`COMMANDS.md`](COMMANDS.md) | Comandos de dev e produção |

## Variáveis principais

| Variável | Uso |
|---|---|
| `MONGODB_URI` | Conexão MongoDB |
| `JWT_SECRET` | Assinatura JWT (≥ 32 chars) |
| `CORS_ALLOWED_ORIGINS` | Origins permitidas (CSV) |
| `VITE_API_BASE_URL` | Base URL da API no frontend |
| `VITE_BASE` | Base path do Vite (`/` local; `/bloodmatch-frontend/` no GitHub Pages) |
| `APP_PUBLIC_URL` | URL do frontend nos e-mails de confirmação |
| `REQUIRE_EMAIL_CONFIRMATION` | Exige confirmar e-mail antes do login |
| `GOOGLE_MAPS_API_KEY` | Geocoding (distância nas recomendações) |
| `SPRING_MAIL_*` | SMTP da confirmação de e-mail |
