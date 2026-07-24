#!/usr/bin/env bash

set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
SEED_PASSWORD="${SEED_PASSWORD:-Senha12345!}"
TODAY=$(date +%F)

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

days_from_today() {
  date -d "$TODAY + $1 days" +%F
}

post() {
  local path="$1"
  local token="${2:-}"
  local body="$3"

  if [[ -n "$token" ]]; then
    curl --fail-with-body -sS -X POST "$API_URL$path" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $token" \
      -d "$body"
  else
    curl --fail-with-body -sS -X POST "$API_URL$path" \
      -H "Content-Type: application/json" \
      -d "$body"
  fi
}

patch() {
  local path="$1"
  local token="$2"
  local body="$3"

  curl --fail-with-body -sS -X PATCH "$API_URL$path" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "$body"
}

token_for() {
  local email="$1"

  post /auth/login "" \
    "{\"email\":\"$email\",\"password\":\"$SEED_PASSWORD\"}" \
    | jq -r '.accessToken'
}

require_command curl
require_command jq

echo "Seeding via API: $API_URL (data base: $TODAY)"

DONORS_DATA=$'Ana Silva|12345678901|1998-05-10|ana.silva@blood.local|O-|78.0|Avenida Paulista|1578|Bela Vista|São Paulo|SP|01310-200\nBruno Santos|98765432100|1997-08-21|bruno.santos@blood.local|O+|82.0|Rua Augusta|1449|Consolação|São Paulo|SP|01305-100\nCarla Oliveira|11122233344|1996-04-14|carla.oliveira@blood.local|A-|74.0|Rua Haddock Lobo|595|Jardins|São Paulo|SP|01414-001\nDaniel Costa|22233344455|1999-09-02|daniel.costa@blood.local|A+|68.5|Rua da Consolação|2300|Consolação|São Paulo|SP|01302-001\nFernanda Lima|33344455566|1995-12-18|fernanda.lima@blood.local|B-|80.0|Rua Oscar Freire|379|Jardins|São Paulo|SP|01426-001\nGabriel Almeida|44455566677|2000-03-23|gabriel.almeida@blood.local|B+|71.5|Rua dos Pinheiros|937|Pinheiros|São Paulo|SP|05422-012\nHelena Rocha|55566677788|1997-07-11|helena.rocha@blood.local|AB-|76.0|Rua Teodoro Sampaio|1020|Pinheiros|São Paulo|SP|05406-050\nIgor Pereira|66677788899|1998-11-30|igor.pereira@blood.local|AB+|69.0|Rua Vergueiro|1000|Vila Mariana|São Paulo|SP|01504-000\nJuliana Martins|77788899900|2001-01-19|juliana.martins@blood.local|O+|77.0|Avenida Brigadeiro Faria Lima|2232|Jardim Paulistano|São Paulo|SP|01451-000\nLucas Ferreira|88899900011|2002-06-27|lucas.ferreira@blood.local|A+|66.0|Rua Bela Cintra|934|Jardins|São Paulo|SP|01415-000'
ORGS_DATA=$'Fundação Pró-Sangue Clínicas|12345678000100|hemo1@blood.local|Avenida Doutor Enéas Carvalho de Aguiar|155|Cerqueira César|São Paulo|SP|05403-000\nFundação Pró-Sangue Mandaqui|12345678000101|hemo2@blood.local|Rua Voluntários da Pátria|4227|Santana|São Paulo|SP|02401-400\nHospital São Paulo|12345678000102|hemo3@blood.local|Rua Napoleão de Barros|715|Vila Clementino|São Paulo|SP|04024-002\nHospital Beneficência Portuguesa|12345678000103|hemo4@blood.local|Rua Maestro Cardim|769|Bela Vista|São Paulo|SP|01323-001\nHospital Alemão Oswaldo Cruz|12345678000104|hemo5@blood.local|Rua João Julião|331|Bela Vista|São Paulo|SP|01323-903\nHospital das Clínicas FMUSP|12345678000105|hemo6@blood.local|Avenida Doutor Enéas Carvalho de Aguiar|255|Cerqueira César|São Paulo|SP|05403-000\nHospital do Servidor Público Estadual|12345678000106|hemo7@blood.local|Rua Pedro de Toledo|1800|Vila Clementino|São Paulo|SP|04039-901\nHospital Santa Cruz|12345678000107|hemo8@blood.local|Rua Santa Cruz|398|Vila Mariana|São Paulo|SP|04122-000\nHospital Israelita Albert Einstein|12345678000108|hemo9@blood.local|Avenida Albert Einstein|627|Morumbi|São Paulo|SP|05652-900\nHospital Sírio-Libanês|12345678000109|hemo10@blood.local|Rua Dona Adma Jafet|91|Bela Vista|São Paulo|SP|01308-050'

DONOR_PERSON_IDS=()
DONOR_EMAILS=()
DONOR_BLOOD_TYPES=()
ORG_IDS=()
ORG_EMAILS=()

while IFS='|' read -r name cpf birth_date email blood_type weight street number neighborhood city state zip_code; do
  [[ -z "$name" ]] && continue

  person_id=$(
    post /parties/persons "" \
      "{\"name\":\"$name\",\"cpf\":\"$cpf\",\"birthDate\":\"$birth_date\",\"email\":\"$email\",\"password\":\"$SEED_PASSWORD\",\"passwordConfirmation\":\"$SEED_PASSWORD\",\"street\":\"$street\",\"number\":\"$number\",\"neighborhood\":\"$neighborhood\",\"city\":\"$city\",\"state\":\"$state\",\"zipCode\":\"$zip_code\"}" \
      | jq -r '.id'
  )

  pretoken=$(token_for "$email")

  post /donors "$pretoken" \
    "{\"personId\":\"$person_id\",\"bloodType\":\"$blood_type\",\"weight\":$weight}" \
    >/dev/null

  post /requesters "$pretoken" \
    "{\"partyId\":\"$person_id\"}" \
    >/dev/null

  DONOR_PERSON_IDS+=("$person_id")
  DONOR_EMAILS+=("$email")
  DONOR_BLOOD_TYPES+=("$blood_type")

done <<< "$DONORS_DATA"

while IFS='|' read -r name cnpj email street number neighborhood city state zip_code; do
  [[ -z "$name" ]] && continue

  org_id=$(
    post /parties/organizations "" \
      "{\"name\":\"$name\",\"cnpj\":\"$cnpj\",\"email\":\"$email\",\"password\":\"$SEED_PASSWORD\",\"passwordConfirmation\":\"$SEED_PASSWORD\",\"street\":\"$street\",\"number\":\"$number\",\"neighborhood\":\"$neighborhood\",\"city\":\"$city\",\"state\":\"$state\",\"zipCode\":\"$zip_code\"}" \
      | jq -r '.id'
  )

  pretoken=$(token_for "$email")

  post /requesters "$pretoken" \
    "{\"partyId\":\"$org_id\"}" \
    >/dev/null

  post /blood-centers "$pretoken" \
    "{\"organizationId\":\"$org_id\"}" \
    >/dev/null

  ORG_IDS+=("$org_id")
  ORG_EMAILS+=("$email")

done <<< "$ORGS_DATA"

# Tipos compatíveis com doadores no mesmo hemocentro (i % 10)
donor_request_types=("A+" "B+" "A+" "A+" "B+" "B+" "AB+" "AB+" "O+" "A+")
org_request_types=("O+" "A+" "B+" "AB+" "O-" "A-" "B-" "AB-" "A+" "B+")

donor_urgencies=("critical" "medium" "low" "critical" "medium" "low" "critical" "medium" "low" "low")
org_urgencies=("medium" "critical" "low" "critical" "medium" "low" "critical" "low" "medium" "critical")

donor_goal_bags=(2 3 1 4 2 3 1 2 3 1)
org_goal_bags=(5 10 3 8 6 4 12 5 7 9)

created_request_ids=()

# =========================
# REQUESTS DE DOADORES
# dateRequested = hoje (via API)
# dateLimit = hoje + N dias
# =========================

for i in "${!DONOR_PERSON_IDS[@]}"; do
  requester_token=$(token_for "${DONOR_EMAILS[$i]}")
  requester_id="${DONOR_PERSON_IDS[$i]}"
  blood_center_id="${ORG_IDS[$((i % ${#ORG_IDS[@]}))]}"
  request_limit=$(days_from_today $((i + 7)))

  request_body="{\"partyId\":\"$requester_id\",\"organizationId\":\"$blood_center_id\",\"bloodTypeNeeded\":\"${donor_request_types[$i]}\",\"dateLimit\":\"$request_limit\",\"urgency\":\"${donor_urgencies[$i]}\",\"goalBloodBags\":${donor_goal_bags[$i]}}"

  request_id=$(post /donation-requests "$requester_token" "$request_body" | jq -r '.id')

  created_request_ids+=("$request_id")
done

# =========================
# REQUESTS DE HEMOCENTROS
# =========================

for i in "${!ORG_IDS[@]}"; do
  requester_token=$(token_for "${ORG_EMAILS[$i]}")
  requester_id="${ORG_IDS[$i]}"
  blood_center_id="${ORG_IDS[$i]}"
  request_limit=$(days_from_today $((i + 10)))

  request_body="{\"partyId\":\"$requester_id\",\"organizationId\":\"$blood_center_id\",\"bloodTypeNeeded\":\"${org_request_types[$i]}\",\"dateLimit\":\"$request_limit\",\"urgency\":\"${org_urgencies[$i]}\",\"goalBloodBags\":${org_goal_bags[$i]}}"

  request_id=$(post /donation-requests "$requester_token" "$request_body" | jq -r '.id')

  created_request_ids+=("$request_id")
done

# =========================
# DOAÇÕES CONCLUÍDAS (HOJE)
#
# Regras de fulfillment do domínio:
# - mesma data ou posterior à dateRequested (hoje)
# - mesma data ou anterior à dateLimit
# - mesmo hemocentro
# - doação concluída + tipo sanguíneo compatível
# - distribuição FIFO entre requests do mesmo centro
#
# Formato: donor_index|org_index
# Doações extras no mesmo centro preenchem parcial/totalmente requests.
# Doadores 5-9 ficam elegíveis (sem doação recente) para testar recomendações.
# =========================

completed_donation_ids=()

DONATION_SCENARIOS=$'0|0\n1|0\n8|0\n1|1\n3|1\n8|1\n2|2\n6|2\n0|3\n4|3'

while IFS='|' read -r donor_idx org_idx; do
  [[ -z "$donor_idx" ]] && continue

  donor_id="${DONOR_PERSON_IDS[$donor_idx]}"
  donor_token=$(token_for "${DONOR_EMAILS[$donor_idx]}")
  blood_center_id="${ORG_IDS[$org_idx]}"

  donation_id=$(
    post /donations/completed \
      "$donor_token" \
      "{\"personId\":\"$donor_id\",\"bloodCenterId\":\"$blood_center_id\",\"donationDate\":\"$TODAY\"}" \
      | jq -r '.id'
  )

  completed_donation_ids+=("$donation_id")
done <<< "$DONATION_SCENARIOS"

donor_logins=$(printf "%s\n" "$DONORS_DATA" | awk -F'|' '{print "- " $4}')
org_logins=$(printf "%s\n" "$ORGS_DATA" | awk -F'|' '{print "- " $3}')

cat <<EOF
Seed concluído.

Data de referência: $TODAY

Doadores: ${#DONOR_PERSON_IDS[@]}
Hemocentros/hospitais: ${#ORG_IDS[@]}
Requests criados: ${#created_request_ids[@]}
Doações concluídas (hoje): ${#completed_donation_ids[@]}

Critérios de teste (fulfillment):
- Cada request aponta para o próprio hemocentro do solicitante.
- A alocação é FIFO por data e hora de criação; cada bolsa atende apenas uma request compatível.
- hemo1–hemo4 possuem doações concluídas hoje para validar metas atingidas, parciais e abertas.
- hemo5–hemo10 não possuem doações concluídas para validar requests abertas sem progresso.
- doadores Gabriel, Igor e Lucas: elegíveis para novas doações/recomendações

Senha padrão: $SEED_PASSWORD

Logins de doadores:
$donor_logins

Logins de hemocentros/hospitais:
$org_logins
EOF
