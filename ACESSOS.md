# 🔗 Links e Acessos do Projeto BloodMatch

Guia rápido de URLs, portas, endpoints e configurações de acesso do projeto **BloodMatch**.

---

## 🌐 Serviços Locais (Ambiente de Desenvolvimento)

| Serviço | URL / Endereço | Descrição |
|---|---|---|
| **Frontend (Dev)** | [http://localhost:5173](http://localhost:5173) | Interface do usuário em desenvolvimento (React + Vite) |
| **Backend API** | [http://localhost:8080](http://localhost:8080) | API REST Spring Boot |
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | Documentação interativa da API |
| **OpenAPI Specs** | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) | Especificação OpenAPI em JSON |

---

## 🏭 Serviços de Produção (VPS / Hostinger)

Subindo com `docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.production up -d --build`:

| Serviço | URL / Endereço Direto | Porta Host | Descrição |
|---|---|---|---|
| **Frontend SPA (Nginx)** | [http://179.198.120.172:8082](http://179.198.120.172:8082) | `8082` | Proxy reverso Nginx servindo o frontend SPA |
| **Backend API** | [http://179.198.120.172:8080](http://179.198.120.172:8080) | `8080` | Container da API Java Spring Boot |
| **Swagger UI (Produção)** | [http://179.198.120.172:8080/swagger-ui.html](http://179.198.120.172:8080/swagger-ui.html) | `8080` | Documentação interativa Swagger na VPS |
| **Domínio Customizado** | `http://meubloodmatch.com` | `80` / `443` | Roteado via Nginx principal da VPS |

---

## 📁 Estrutura de Infraestrutura Adicionada

- **Nginx Config:** [`infrastructure/nginx/default.conf`](file:///c:/Users/garde/Desktop/projects/blood-match/infrastructure/nginx/default.conf)
- **Compose de Produção:** [`docker-compose.prod.yml`](file:///c:/Users/garde/Desktop/projects/blood-match/docker-compose.prod.yml)
- **Env de Produção Exemplo:** [`.env.production.example`](file:///c:/Users/garde/Desktop/projects/blood-match/.env.production.example)

---

## 📋 Comandos Rápidos de Deploy

### Desenvolvimento:
```bash
docker compose up --build
```

### Produção VPS (Hostinger):
```bash
cp .env.production.example .env.production
cp .env.production .env

docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.production up -d --build
```
