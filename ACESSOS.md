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

---

## 🔑 Credenciais e Logins de Teste (Seed Data)

> **Senha padrão para todos os usuários:** `Senha12345!`

### 🩸 Doadores de Sangue (Pessoas Físicas)

| Nome | E-mail / Login | Tipo Sanguíneo |
|---|---|---|
| Ana Silva | `ana.silva@blood.local` | `O-` |
| Bruno Santos | `bruno.santos@blood.local` | `O+` |
| Carla Oliveira | `carla.oliveira@blood.local` | `A-` |
| Daniel Costa | `daniel.costa@blood.local` | `A+` |
| Fernanda Lima | `fernanda.lima@blood.local` | `B-` |
| Gabriel Almeida | `gabriel.almeida@blood.local` | `B+` |
| Helena Rocha | `helena.rocha@blood.local` | `AB-` |
| Igor Pereira | `igor.pereira@blood.local` | `AB+` |
| Juliana Martins | `juliana.martins@blood.local` | `O+` |
| Lucas Ferreira | `lucas.ferreira@blood.local` | `A+` |

### 🏥 Hemocentros / Hospitais (Organizações)

| Nome da Instituição | E-mail / Login |
|---|---|
| Hospital Ferreira Machado | `hemo1@blood.local` |
| Hemocentro Regional de Campos | `hemo2@blood.local` |
| Hospital Geral Benê (Beneficência Portuguesa) | `hemo3@blood.local` |
| Núcleo Medicina Transfusional (Banco de Sangue) | `hemo4@blood.local` |
| Santa Casa de Misericórdia de Campos | `hemo5@blood.local` |
| Hospital Unimed Campos | `hemo6@blood.local` |
| Hospital dos Plantadores de Cana | `hemo7@blood.local` |
| Hospital Geral de Guarus | `hemo8@blood.local` |
| Hospital Geral Dr. Beda | `hemo9@blood.local` |
| Hospital Escola Álvaro Alvim | `hemo10@blood.local` |
