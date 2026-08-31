# Comandos úteis BloodMatch

## Desenvolvimento local (Docker)

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

## Produção (VPS / Hostinger)

```bash
cp .env.production.example .env.production
# edite JWT_SECRET e variáveis se necessário

docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.production up -d --build
```

- App / Nginx: http://localhost:8082

## Seed

```bash
./scripts/seed-dev.sh
```

## Simulação de uso

```bash
python scripts/simulate-usage.py --self-test
python scripts/simulate-usage.py --scenario all --report sim-report.json
```

Contrato da API: [`docs/FRONTEND_API_CONTRACT.md`](docs/FRONTEND_API_CONTRACT.md).
