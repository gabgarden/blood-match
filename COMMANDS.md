# Comandos úteis BloodMatch

## Desenvolvimento Local (Docker)
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
- App / Nginx Proxy: http://localhost:8082



command test concurrency

python scripts/simulate-usage.py --self-test
python scripts/simulate-usage.py --scenario all --report sim-report.json