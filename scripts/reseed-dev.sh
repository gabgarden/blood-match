#!/usr/bin/env bash

# =============================================================================
# reseed-dev.sh - Limpa o banco e reinsere dados de desenvolvimento
#
# Uso:
#   ./scripts/reseed-dev.sh                   # pede confirmacao
#   ./scripts/reseed-dev.sh --yes             # sem confirmacao (CI/CD)
#   SEED_PASSWORD=OutraSenha ./scripts/reseed-dev.sh
#   API_URL=http://localhost:8081 ./scripts/reseed-dev.sh
#
# Requisitos: curl, jq, docker (para limpeza do MongoDB)
# =============================================================================

set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
SEED_PASSWORD="${SEED_PASSWORD:-Senha12345!}"
MONGODB_URI="${MONGODB_URI:-}"
MONGODB_DATABASE="${MONGODB_DATABASE:-bloodmatch}"
TODAY=$(date +%F)
SKIP_CONFIRM=""
NO_CLEAN=""
for arg in "$@"; do
  case "$arg" in
    --yes)      SKIP_CONFIRM="--yes" ;;
    --no-clean) NO_CLEAN="--no-clean" ;;
  esac
done

ok()   { echo "  [ok]  $*"; }
info() { echo "  [...] $*"; }
warn() { echo "  [!]   $*"; }
fail() { echo "  [ERR] $*" >&2; exit 1; }
step() { echo ""; echo "=== $* ==="; }

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Comando nao encontrado: $1"
}

days_from_today() {
  if date -d "tomorrow" >/dev/null 2>&1; then
    date -d "$TODAY + $1 days" +%F
  else
    date -v "+${1}d" -j -f "%Y-%m-%d" "$TODAY" +%F
  fi
}

api_post() {
  local path="$1" token="${2:-}" body="$3" label="${4:-$path}"
  if [[ -n "$token" ]]; then
    curl --fail-with-body -sS -X POST "$API_URL$path" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $token" \
      -d "$body" || fail "Falha em POST $label"
  else
    curl --fail-with-body -sS -X POST "$API_URL$path" \
      -H "Content-Type: application/json" \
      -d "$body" || fail "Falha em POST $label"
  fi
}

token_for() {
  api_post /auth/login "" \
    "{\"email\":\"$1\",\"password\":\"$SEED_PASSWORD\"}" \
    "login $1" | jq -r '.accessToken'
}

# Detecta o mongosh disponivel (local ou via docker)
MONGOSH_CMD=""
if command -v mongosh >/dev/null 2>&1; then
  MONGOSH_CMD="mongosh"
elif command -v docker >/dev/null 2>&1; then
  MONGOSH_CMD="docker run --rm mongo:8 mongosh"
fi

require_command curl
require_command jq

step "BloodMatch Reseed Dev"
info "API      : $API_URL"
info "Banco    : $MONGODB_DATABASE"
info "Data base: $TODAY"
info "Senha    : $SEED_PASSWORD"

if [[ "$SKIP_CONFIRM" != "--yes" ]]; then
  echo ""
  warn "ATENCAO: todos os dados de '$MONGODB_DATABASE' serao APAGADOS e recriados."
  read -r -p "  Confirmar? [s/N] " CONFIRM
  [[ "${CONFIRM,,}" == "s" ]] || { echo "Cancelado."; exit 0; }
fi

# --- 1. Limpeza do banco ---
step "1/5  Limpando banco de dados"

if [[ -z "$MONGODB_URI" ]]; then
  ENV_FILE="$(cd "$(dirname "$0")/.." && pwd)/.env"
  if [[ -f "$ENV_FILE" ]]; then
    MONGODB_URI=$(grep -E '^MONGODB_URI=' "$ENV_FILE" | head -1 | cut -d= -f2-)
    _db=$(grep -E '^MONGODB_DATABASE=' "$ENV_FILE" | head -1 | cut -d= -f2-) && MONGODB_DATABASE="${_db:-bloodmatch}"
  fi
fi

if [[ -n "$NO_CLEAN" ]]; then
  warn "Limpeza ignorada (--no-clean). Banco mantido como esta."
elif [[ -z "$MONGODB_URI" ]]; then
  warn "MONGODB_URI nao encontrado — pulando limpeza. Defina MONGODB_URI no .env ou como variavel de ambiente."
elif [[ -z "$MONGOSH_CMD" ]]; then
  warn "mongosh nao encontrado — pulando limpeza automatica."
  warn "Execute manualmente: mongosh \"\$MONGODB_URI\" --eval \"db.getCollectionNames().forEach(c => db[c].drop())\""
else
  info "Conectando ao MongoDB Atlas via: $MONGOSH_CMD ..."
  $MONGOSH_CMD "$MONGODB_URI" --quiet --eval "
    const cols = db.getCollectionNames();
    cols.forEach(c => { db[c].drop(); print('dropped: ' + c); });
    print('Banco limpo: ' + cols.length + ' colecoes removidas.');
  " || fail "Falha ao limpar o banco"
  ok "Banco limpo"
fi

# --- 2. Aguarda API ---
step "2/5  Verificando disponibilidade da API"
MAX_TRIES=15
for i in $(seq 1 $MAX_TRIES); do
  if curl -sf "$API_URL/v3/api-docs" >/dev/null 2>&1; then
    ok "API disponivel"
    break
  fi
  [[ $i -eq $MAX_TRIES ]] && fail "API nao respondeu apos ${MAX_TRIES}s — container rodando?"
  info "Aguardando API... ($i/${MAX_TRIES})"
  sleep 2
done

# --- Dados ---
DONORS_DATA=$'Ana Silva|12345678901|1998-05-10|ana.silva@blood.local|O-|78.0|22999990001|Rua Rocha Leao, 2 - Caju|Campos dos Goytacazes|RJ|28035-045\nBruno Santos|98765432100|1997-08-21|bruno.santos@blood.local|O+|82.0|22999990002|Rua Barao de Miracema, 140 - Centro|Campos dos Goytacazes|RJ|28035-562\nCarla Oliveira|11122233344|1996-04-14|carla.oliveira@blood.local|A-|74.0|22999990003|Rua Visconde de Itaborai, 402 - Parque Rosario|Campos dos Goytacazes|RJ|28010-295\nDaniel Costa|22233344455|1999-09-02|daniel.costa@blood.local|A+|68.5|22999990004|Avenida Pelinca, 115 - Parque Tamandare|Campos dos Goytacazes|RJ|28035-053\nFernanda Lima|33344455566|1995-12-18|fernanda.lima@blood.local|B-|80.0|22999990005|Rua Visconde de Itaborai, 427 - Parque Rosario|Campos dos Goytacazes|RJ|28010-295\nGabriel Almeida|44455566677|2000-03-23|gabriel.almeida@blood.local|B+|71.5|22999990006|Avenida Jose Alves de Azevedo, 337 - Parque Rosario|Campos dos Goytacazes|RJ|28025-497\nHelena Rocha|55566677788|1997-07-11|helena.rocha@blood.local|AB-|76.0|22999990007|Avenida Senador Jose Carlos Pereira Pinto, 400 - Parque Calabouco|Campos dos Goytacazes|RJ|28080-000\nIgor Pereira|66677788899|1998-11-30|igor.pereira@blood.local|AB+|69.0|22999990008|Rua Conselheiro Otaviano, 129 - Centro|Campos dos Goytacazes|RJ|28010-140\nJuliana Martins|77788899900|2001-01-19|juliana.martins@blood.local|O+|77.0|22999990009|Rua Barao da Lagoa Dourada, 409 - Centro|Campos dos Goytacazes|RJ|28035-211\nLucas Ferreira|88899900011|2002-06-27|lucas.ferreira@blood.local|A+|66.0|22999990010|Rua Rocha Leao, 2 - Caju|Campos dos Goytacazes|RJ|28035-045'

ORGS_DATA=$'Hospital Ferreira Machado|12345678000100|hemo1@blood.local|22988880001|Rua Rocha Leao, 2 - Caju|Campos dos Goytacazes|RJ|28035-045\nHemocentro Regional de Campos|12345678000101|hemo2@blood.local|22988880002|Rua Rocha Leao, 2 - Caju|Campos dos Goytacazes|RJ|28035-045\nHospital Geral Bene Beneficencia Portuguesa|12345678000102|hemo3@blood.local|22988880003|Rua Barao de Miracema, 140 - Centro|Campos dos Goytacazes|RJ|28035-562\nNucleo Medicina Transfusional Banco de Sangue|12345678000103|hemo4@blood.local|22988880004|Rua Conselheiro Otaviano, 129 - Centro|Campos dos Goytacazes|RJ|28010-140\nSanta Casa de Misericordia de Campos|12345678000104|hemo5@blood.local|22988880005|Rua Voluntarios da Patria, 427 - Centro|Campos dos Goytacazes|RJ|28010-295\nHospital Unimed Campos|12345678000105|hemo6@blood.local|22988880006|Avenida Pelinca, 115 - Parque Tamandare|Campos dos Goytacazes|RJ|28035-053\nHospital dos Plantadores de Cana|12345678000106|hemo7@blood.local|22988880007|Avenida Jose Alves de Azevedo, 337 - Parque Rosario|Campos dos Goytacazes|RJ|28025-497\nHospital Geral de Guarus|12345678000107|hemo8@blood.local|22988880008|Avenida Senador Jose Carlos Pereira Pinto, 400 - Parque Calabouco|Campos dos Goytacazes|RJ|28080-000\nHospital Geral Dr. Beda|12345678000108|hemo9@blood.local|22988880009|Rua Conselheiro Otaviano, 129 - Centro|Campos dos Goytacazes|RJ|28010-140\nHospital Escola Alvaro Alvim|12345678000109|hemo10@blood.local|22988880010|Rua Barao da Lagoa Dourada, 409 - Centro|Campos dos Goytacazes|RJ|28035-211'

# --- 3. Registro de doadores e hemocentros ---
step "3/5  Registrando doadores e hemocentros"

DONOR_PERSON_IDS=()
DONOR_EMAILS=()
ORG_IDS=()
ORG_EMAILS=()

DONOR_COUNT=0
while IFS='|' read -r name cpf birth_date email blood_type weight phone_number street city state zip_code; do
  [[ -z "$name" ]] && continue

  person_id=$(
    api_post /parties/persons "" \
      "{\"name\":\"$name\",\"cpf\":\"$cpf\",\"birthDate\":\"$birth_date\",\"email\":\"$email\",\"password\":\"$SEED_PASSWORD\",\"passwordConfirmation\":\"$SEED_PASSWORD\",\"phoneNumber\":\"$phone_number\",\"street\":\"$street\",\"city\":\"$city\",\"state\":\"$state\",\"zipCode\":\"$zip_code\"}" \
      "registro $name" | jq -r '.id'
  )

  pretoken=$(token_for "$email")

  api_post /donors "$pretoken" \
    "{\"personId\":\"$person_id\",\"bloodType\":\"$blood_type\",\"weight\":$weight}" \
    "donor $name" >/dev/null

  api_post /requesters "$pretoken" \
    "{\"partyId\":\"$person_id\"}" \
    "requester $name" >/dev/null

  DONOR_PERSON_IDS+=("$person_id")
  DONOR_EMAILS+=("$email")
  DONOR_COUNT=$((DONOR_COUNT + 1))
  ok "Doador $DONOR_COUNT: $name ($blood_type)"

done <<< "$DONORS_DATA"

ORG_COUNT=0
while IFS='|' read -r name cnpj email phone_number street city state zip_code; do
  [[ -z "$name" ]] && continue

  org_id=$(
    api_post /parties/organizations "" \
      "{\"name\":\"$name\",\"cnpj\":\"$cnpj\",\"email\":\"$email\",\"password\":\"$SEED_PASSWORD\",\"passwordConfirmation\":\"$SEED_PASSWORD\",\"phoneNumber\":\"$phone_number\",\"street\":\"$street\",\"city\":\"$city\",\"state\":\"$state\",\"zipCode\":\"$zip_code\"}" \
      "registro $name" | jq -r '.id'
  )

  pretoken=$(token_for "$email")

  api_post /requesters "$pretoken" \
    "{\"partyId\":\"$org_id\"}" \
    "requester $name" >/dev/null

  api_post /blood-centers "$pretoken" \
    "{\"organizationId\":\"$org_id\"}" \
    "blood-center $name" >/dev/null

  ORG_IDS+=("$org_id")
  ORG_EMAILS+=("$email")
  ORG_COUNT=$((ORG_COUNT + 1))
  ok "Hemocentro $ORG_COUNT: $name"

done <<< "$ORGS_DATA"

# --- 4. Pedidos de doacao ---
step "4/5  Criando pedidos de doacao (datas relativas a $TODAY)"

donor_request_types=("A+" "B+" "A+" "A+" "B+" "B+" "AB+" "AB+" "O+" "A+")
org_request_types=("O+" "A+" "B+" "AB+" "O-" "A-" "B-" "AB-" "A+" "B+")
donor_urgencies=("critical" "medium" "low" "critical" "medium" "low" "critical" "medium" "low" "low")
org_urgencies=("medium" "critical" "low" "critical" "medium" "low" "critical" "low" "medium" "critical")
donor_goal_bags=(2 3 1 4 2 3 1 2 3 1)
org_goal_bags=(5 10 3 8 6 4 12 5 7 9)

created_request_ids=()

for i in "${!DONOR_PERSON_IDS[@]}"; do
  requester_token=$(token_for "${DONOR_EMAILS[$i]}")
  requester_id="${DONOR_PERSON_IDS[$i]}"
  blood_center_id="${ORG_IDS[$((i % ${#ORG_IDS[@]}))]}"
  request_limit=$(days_from_today $((i + 7)))

  request_id=$(api_post /donation-requests "$requester_token" \
    "{\"partyId\":\"$requester_id\",\"organizationId\":\"$blood_center_id\",\"bloodTypeNeeded\":\"${donor_request_types[$i]}\",\"dateLimit\":\"$request_limit\",\"urgency\":\"${donor_urgencies[$i]}\",\"goalBloodBags\":${donor_goal_bags[$i]}}" \
    "request doador $i" | jq -r '.id')

  created_request_ids+=("$request_id")
  ok "Request doador $((i+1)): ${donor_request_types[$i]} urgencia=${donor_urgencies[$i]} limite=$request_limit"
done

for i in "${!ORG_IDS[@]}"; do
  requester_token=$(token_for "${ORG_EMAILS[$i]}")
  requester_id="${ORG_IDS[$i]}"
  blood_center_id="${ORG_IDS[$i]}"
  request_limit=$(days_from_today $((i + 10)))

  request_id=$(api_post /donation-requests "$requester_token" \
    "{\"partyId\":\"$requester_id\",\"organizationId\":\"$blood_center_id\",\"bloodTypeNeeded\":\"${org_request_types[$i]}\",\"dateLimit\":\"$request_limit\",\"urgency\":\"${org_urgencies[$i]}\",\"goalBloodBags\":${org_goal_bags[$i]}}" \
    "request hemocentro $i" | jq -r '.id')

  created_request_ids+=("$request_id")
  ok "Request hemocentro $((i+1)): ${org_request_types[$i]} urgencia=${org_urgencies[$i]} limite=$request_limit"
done

# --- 5. Doacoes ---
step "5/5  Registrando doacoes concluidas (hoje: $TODAY)"

# hemo1-4 recebem doacoes para validar metas
# Gabriel(idx=5), Igor(idx=7), Lucas(idx=9) ficam elegiveis para recomendacoes
DONATION_SCENARIOS=$'0|0\n1|0\n8|0\n1|1\n3|1\n8|1\n2|2\n6|2\n0|3\n4|3'

DON_COUNT=0
while IFS='|' read -r donor_idx org_idx; do
  [[ -z "$donor_idx" ]] && continue

  donor_token=$(token_for "${DONOR_EMAILS[$donor_idx]}")

  api_post /donations "$donor_token" \
    "{\"personId\":\"${DONOR_PERSON_IDS[$donor_idx]}\",\"organizationId\":\"${ORG_IDS[$org_idx]}\",\"donationDate\":\"$TODAY\"}" \
    "donation donor[$donor_idx]->org[$org_idx]" >/dev/null

  DON_COUNT=$((DON_COUNT + 1))
  ok "Doacao $DON_COUNT: ${DONOR_EMAILS[$donor_idx]} -> ${ORG_EMAILS[$org_idx]}"

done <<< "$DONATION_SCENARIOS"

# --- Resumo ---
echo ""
echo "========================================"
echo "  Reseed concluido com sucesso!"
echo "========================================"
echo ""
printf "  %-22s %s\n" "Data de referencia:" "$TODAY"
printf "  %-22s %s\n" "Doadores:"           "${#DONOR_PERSON_IDS[@]}"
printf "  %-22s %s\n" "Hemocentros:"        "${#ORG_IDS[@]}"
printf "  %-22s %s\n" "Requests criadas:"   "${#created_request_ids[@]}"
printf "  %-22s %s\n" "Doacoes de hoje:"    "$DON_COUNT"
printf "  %-22s %s\n" "Senha padrao:"       "$SEED_PASSWORD"
echo ""
echo "Logins de doadores:"
printf '%s\n' "$DONORS_DATA" | awk -F'|' '{printf "  %-32s tipo: %s\n", $4, $5}'
echo ""
echo "Logins de hemocentros:"
printf '%s\n' "$ORGS_DATA"   | awk -F'|' '{printf "  %s\n", $3}'
echo ""
echo "Criterios de teste:"
echo "  hemo1-hemo4  -> tem doacoes hoje (metas atingidas/parciais)"
echo "  hemo5-hemo10 -> sem doacoes (requests abertas sem progresso)"
echo "  Gabriel, Igor, Lucas -> elegiveis para recomendacoes"