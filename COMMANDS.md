# Comandos úteis BloodMatch

## Desenvolvimento local (Docker)

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

O gateway Nginx **não** sobe no compose de desenvolvimento. O browser chama a API em `VITE_API_BASE_URL` (padrão `http://localhost:8080`).

## Produção (VPS / Hostinger)

```bash
cp .env.production.example .env.production
# edite JWT_SECRET, MONGODB_URI, SPRING_MAIL_* e GOOGLE_MAPS_API_KEY

docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.production up -d --build
```

- App / Nginx: http://localhost:8082

## Seed

Com a API no ar:

```bash
./scripts/seed-dev.sh
```

Contrato da API: [`docs/FRONTEND_API_CONTRACT.md`](docs/FRONTEND_API_CONTRACT.md).
