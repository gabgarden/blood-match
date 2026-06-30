#!/usr/bin/env bash

set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
SEED_PASSWORD="${SEED_PASSWORD:-Senha12345!}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

post() {
  local path="$1"
  local token="${2:-}"
  local body="$3"

  if [[ -n "$token" ]]; then
    curl -sS -X POST "$API_URL$path" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $token" \
      -d "$body"
  else
    curl -sS -X POST "$API_URL$path" \
      -H "Content-Type: application/json" \
      -d "$body"
  fi
}

patch() {
  local path="$1"
  local token="$2"
  local body="$3"

  curl -sS -X PATCH "$API_URL$path" \
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

echo "Seeding via API: $API_URL"

DONORS_DATA=$'Ana Silva|12345678901|1998-05-10|ana.silva@blood.local|O-|78.0|Rua das Flores 123|São Paulo|SP|01001-000\nBruno Santos|98765432100|1997-08-21|bruno.santos@blood.local|O+|82.0|Avenida Brasil 456|São Paulo|SP|01002-000\nCarla Oliveira|11122233344|1996-04-14|carla.oliveira@blood.local|A-|74.0|Rua Augusta 789|São Paulo|SP|01003-000\nDaniel Costa|22233344455|1999-09-02|daniel.costa@blood.local|A+|68.5|Rua da Consolação 12|São Paulo|SP|01004-000\nFernanda Lima|33344455566|1995-12-18|fernanda.lima@blood.local|B-|80.0|Rua Haddock Lobo 34|São Paulo|SP|01005-000\nGabriel Almeida|44455566677|2000-03-23|gabriel.almeida@blood.local|B+|71.5|Rua dos Pinheiros 56|São Paulo|SP|01006-000\nHelena Rocha|55566677788|1997-07-11|helena.rocha@blood.local|AB-|76.0|Rua Verde 78|São Paulo|SP|01007-000\nIgor Pereira|66677788899|1998-11-30|igor.pereira@blood.local|AB+|69.0|Rua Azul 90|São Paulo|SP|01008-000\nJuliana Martins|77788899900|2001-01-19|juliana.martins@blood.local|O+|77.0|Avenida Paulista 101|São Paulo|SP|01009-000\nLucas Ferreira|88899900011|2002-06-27|lucas.ferreira@blood.local|A+|66.0|Rua do Limoeiro 202|São Paulo|SP|01010-000'

ORGS_DATA=$'Hemocentro Central|12345678000100|hemo1@blood.local|Rua dos Jacarandás 100|São Paulo|SP|01100-000\nHemocentro Norte|12345678000101|hemo2@blood.local|Avenida Norte 200|São Paulo|SP|01101-000\nHospital São Lucas|12345678000102|hemo3@blood.local|Rua São Lucas 300|São Paulo|SP|01102-000\nHemocentro Sul|12345678000103|hemo4@blood.local|Rua do Sul 400|São Paulo|SP|01103-000\nHospital Vida|12345678000104|hemo5@blood.local|Avenida Vida 500|São Paulo|SP|01104-000\nHemocentro Leste|12345678000105|hemo6@blood.local|Rua Leste 600|São Paulo|SP|01105-000\nHemocentro Oeste|12345678000106|hemo7@blood.local|Avenida Oeste 700|São Paulo|SP|01106-000\nHospital Santa Cruz|12345678000107|hemo8@blood.local|Praça Santa Cruz 800|São Paulo|SP|01107-000\nHemocentro Vale|12345678000108|hemo9@blood.local|Rua do Vale 900|São Paulo|SP|01108-000\nHospital Esperança|12345678000109|hemo10@blood.local|Avenida Esperança 1000|São Paulo|SP|01109-000'

DONOR_PERSON_IDS=()
DONOR_TOKENS=()
DONOR_BLOOD_TYPES=()
ORG_IDS=()
ORG_TOKENS=()

while IFS='|' read -r name cpf birth_date email blood_type weight street city state zip_code; do
  [[ -z "$name" ]] && continue

  person_id=$(
    post /parties/persons "" \
      "{\"name\":\"$name\",\"cpf\":\"$cpf\",\"birthDate\":\"$birth_date\",\"email\":\"$email\",\"password\":\"$SEED_PASSWORD\",\"passwordConfirmation\":\"$SEED_PASSWORD\",\"street\":\"$street\",\"city\":\"$city\",\"state\":\"$state\",\"zipCode\":\"$zip_code\"}" \
      | jq -r '.id'
  )

  pretoken=$(token_for "$email")

  post /donors "$pretoken" \
    "{\"personId\":\"$person_id\",\"bloodType\":\"$blood_type\",\"weight\":$weight}" \
    >/dev/null

  post /requesters "$pretoken" \
    "{\"partyId\":\"$person_id\"}" \
    >/dev/null

  final_token=$(token_for "$email")

  DONOR_PERSON_IDS+=("$person_id")
  DONOR_TOKENS+=("$final_token")
  DONOR_BLOOD_TYPES+=("$blood_type")

done <<< "$DONORS_DATA"

while IFS='|' read -r name cnpj email street city state zip_code; do
  [[ -z "$name" ]] && continue

  org_id=$(
    post /parties/organizations "" \
      "{\"name\":\"$name\",\"cnpj\":\"$cnpj\",\"email\":\"$email\",\"password\":\"$SEED_PASSWORD\",\"passwordConfirmation\":\"$SEED_PASSWORD\",\"street\":\"$street\",\"city\":\"$city\",\"state\":\"$state\",\"zipCode\":\"$zip_code\"}" \
      | jq -r '.id'
  )

  pretoken=$(token_for "$email")

  post /requesters "$pretoken" \
    "{\"partyId\":\"$org_id\"}" \
    >/dev/null

  final_token=$(token_for "$email")

  ORG_IDS+=("$org_id")
  ORG_TOKENS+=("$final_token")

done <<< "$ORGS_DATA"

accepted_donor_token="${DONOR_TOKENS[0]}"
accepted_donor_id="${DONOR_PERSON_IDS[0]}"

donor_request_types=("A+" "B+" "AB+" "O+" "A-" "B-" "AB-" "O-" "A+" "AB+")
org_request_types=("O+" "A+" "B+" "AB+" "O-" "A-" "B-" "AB-" "A+" "B+")

donor_urgencies=("critical" "medium" "low" "critical" "medium" "low" "critical" "medium" "low" "low")
org_urgencies=("medium" "critical" "low" "critical" "medium" "low" "critical" "low" "medium" "critical")

created_request_ids=()
completed_donation_ids=()

# =========================
# REQUESTS DE DOADORES
# =========================

for i in "${!DONOR_PERSON_IDS[@]}"; do
  requester_token="${DONOR_TOKENS[$i]}"
  requester_id="${DONOR_PERSON_IDS[$i]}"
  blood_center_id="${ORG_IDS[$((i % ${#ORG_IDS[@]}))]}"

  # requests entre HOJE e +9 dias
  request_limit=$(date -d "+$((i)) days" +%F)

  request_body="{\"requesterId\":\"$requester_id\",\"bloodCenterId\":\"$blood_center_id\",\"bloodTypeNeeded\":\"${donor_request_types[$i]}\",\"dateLimit\":\"$request_limit\",\"urgency\":\"${donor_urgencies[$i]}\"}"

  request_id=$(post /donation-requests "$requester_token" "$request_body" | jq -r '.id')

  created_request_ids+=("$request_id")

  if [[ $i -lt 7 ]]; then

    # doações previstas para próximos dias
    expected_date=$(date -d "+$((1 + i)) days" +%F)

    # concluídas recentemente
    completion_date=$(date -d "-$((i)) days" +%F)

    donation_id=$(
      post /donation-requests/accept-and-create-pending \
        "$accepted_donor_token" \
        "{\"requestId\":\"$request_id\",\"donorId\":\"$accepted_donor_id\",\"expectedDate\":\"$expected_date\"}" \
        | jq -r '.id'
    )

    patch /donations/from-request/complete \
      "$accepted_donor_token" \
      "{\"donationId\":\"$donation_id\",\"completionDate\":\"$completion_date\"}" \
      >/dev/null

    completed_donation_ids+=("$donation_id")
  fi
done

# =========================
# REQUESTS DE HEMOCENTROS
# =========================

for i in "${!ORG_IDS[@]}"; do
  requester_token="${ORG_TOKENS[$i]}"
  requester_id="${ORG_IDS[$i]}"
  blood_center_id="${ORG_IDS[$(((i + 1) % ${#ORG_IDS[@]}))]}"

  # próximos dias
  request_limit=$(date -d "+$((2 + i)) days" +%F)

  request_body="{\"requesterId\":\"$requester_id\",\"bloodCenterId\":\"$blood_center_id\",\"bloodTypeNeeded\":\"${org_request_types[$i]}\",\"dateLimit\":\"$request_limit\",\"urgency\":\"${org_urgencies[$i]}\"}"

  request_id=$(post /donation-requests "$requester_token" "$request_body" | jq -r '.id')

  created_request_ids+=("$request_id")

  if [[ $i -lt 7 ]]; then

    expected_date=$(date -d "+$((2 + i)) days" +%F)

    completion_date=$(date -d "-$((i + 1)) days" +%F)

    donation_id=$(
      post /donation-requests/accept-and-create-pending \
        "$accepted_donor_token" \
        "{\"requestId\":\"$request_id\",\"donorId\":\"$accepted_donor_id\",\"expectedDate\":\"$expected_date\"}" \
        | jq -r '.id'
    )

    patch /donations/from-request/complete \
      "$accepted_donor_token" \
      "{\"donationId\":\"$donation_id\",\"completionDate\":\"$completion_date\"}" \
      >/dev/null

    completed_donation_ids+=("$donation_id")
  fi
done

# =========================
# DOAÇÕES EXTERNAS RECENTES
# =========================

external_donation_ids=()

for i in "${!DONOR_PERSON_IDS[@]}"; do
  donor_token="${DONOR_TOKENS[$i]}"
  donor_id="${DONOR_PERSON_IDS[$i]}"
  blood_center_id="${ORG_IDS[$((i % ${#ORG_IDS[@]}))]}"

  # últimas 2 semanas
  donation_date=$(date -d "-$((i + 1)) days" +%F)

  donation_id=$(
    post /donations/external \
      "$donor_token" \
      "{\"donorId\":\"$donor_id\",\"bloodCenterId\":\"$blood_center_id\",\"donationDate\":\"$donation_date\"}" \
      | jq -r '.id'
  )

  external_donation_ids+=("$donation_id")
done

donor_logins=$(printf "%s\n" "$DONORS_DATA" | awk -F'|' '{print "- " $4}')
org_logins=$(printf "%s\n" "$ORGS_DATA" | awk -F'|' '{print "- " $3}')

cat <<EOF
Seed concluído.

Doadores: ${#DONOR_PERSON_IDS[@]}
Hemocentros/hospitais: ${#ORG_IDS[@]}
Requests criados: ${#created_request_ids[@]}
Requests aceitos e concluídos: ${#completed_donation_ids[@]}
Doações externas: ${#external_donation_ids[@]}

Senha padrão: $SEED_PASSWORD

Logins de doadores:
$donor_logins

Logins de hemocentros/hospitais:
$org_logins
EOF