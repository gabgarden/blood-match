# BloodMatch (monorepo)

Plataforma de matching entre doadores e solicitações de sangue.

```
blood-match/
├── backend/          # Spring Boot (Java 17) + MongoDB
├── frontend/         # React + Vite + TypeScript
├── docs/             # Contratos e estratégias
├── scripts/          # Seed e migrações
├── insomnia/         # Coleção de API
├── tcc-latex/        # TCC
├── docker-compose.yml
└── .env.example
```

## Pré-requisitos

- Docker + Docker Compose **ou**
- Java 17 + Maven e Node.js 22+

## Configuração

```bash
cp .env.example .env
# edite MONGODB_URI, JWT_SECRET, GMAIL_*, GOOGLE_MAPS_API_KEY
```

## Subir com Docker Compose

```bash
docker compose up --build
```

- Frontend (Vite): http://localhost:5173  
- Backend API: http://localhost:8080  
- Swagger: http://localhost:8080/swagger-ui.html  

O browser chama a API em `VITE_API_BASE_URL` (padrão `http://localhost:8080`).

### Build de produção (frontend estático + API)

```bash
docker compose -f docker-compose.prod.yml up --build
```

- Frontend (nginx): http://localhost:3000  
- Backend: http://localhost:8080  

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

## Variáveis principais

| Variável | Uso |
|---|---|
| `MONGODB_URI` | Conexão MongoDB |
| `JWT_SECRET` | Assinatura JWT (≥ 32 chars) |
| `CORS_ALLOWED_ORIGINS` | Origins permitidas (CSV) |
| `VITE_API_BASE_URL` | Base URL da API no frontend |
| `VITE_BASE` | Base path do Vite (`/` local; `/bloodmatch-frontend/` no GitHub Pages) |
