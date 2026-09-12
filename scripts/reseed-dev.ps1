# =============================================================================
# reseed-dev.ps1 — Limpa o banco e reinsere dados de desenvolvimento (Windows)
#
# Uso:
#   .\scripts\reseed-dev.ps1                   # pede confirmacao
#   .\scripts\reseed-dev.ps1 -Yes              # sem confirmacao
#   .\scripts\reseed-dev.ps1 -ApiUrl http://localhost:8081
#   .\scripts\reseed-dev.ps1 -Password MinhaSenh4
#
# Requisitos: Docker Desktop (para limpar o MongoDB Atlas)
# =============================================================================
param(
  [string]$ApiUrl     = "http://localhost:8080",
  [string]$Password   = "Senha12345!",
  [switch]$Yes,
  [switch]$NoClear
)

$ErrorActionPreference = "Stop"
$Today = (Get-Date).ToString("yyyy-MM-dd")

function Days([int]$n) { (Get-Date).AddDays($n).ToString("yyyy-MM-dd") }

function Post([string]$path, [string]$token, [string]$body) {
  $headers = @{ "Content-Type" = "application/json" }
  if ($token) { $headers["Authorization"] = "Bearer $token" }
  try {
    Invoke-RestMethod -Method Post -Uri "$ApiUrl$path" -Headers $headers -Body $body
  } catch {
    $code = $_.Exception.Response.StatusCode.value__
    Write-Warning "POST $path retornou $code`: $($_.ErrorDetails.Message)"
    return $null
  }
}

function Login([string]$email) {
  $r = Post "/auth/login" "" "{`"email`":`"$email`",`"password`":`"$Password`"}"
  return $r.accessToken
}

Write-Host ""
Write-Host "=== BloodMatch Reseed Dev (PowerShell) ===" -ForegroundColor Cyan
Write-Host "  API      : $ApiUrl"
Write-Host "  Banco    : bloodmatch"
Write-Host "  Data base: $Today"
Write-Host "  Senha    : $Password"

if (-not $Yes) {
  Write-Host ""
  Write-Warning "ATENCAO: todos os dados do banco serao APAGADOS e recriados."
  $c = Read-Host "  Confirmar? [s/N]"
  if ($c -ne "s" -and $c -ne "S") { Write-Host "Cancelado."; exit 0 }
}

# --- 1. Limpeza ---
Write-Host ""
Write-Host "=== 1/5  Limpando banco de dados ===" -ForegroundColor Cyan
if ($NoClear) {
  Write-Warning "Limpeza ignorada (-NoClear)."
} else {
  $envFile = Join-Path (Split-Path $PSScriptRoot) ".env"
  $mongoUri = ""
  if (Test-Path $envFile) {
    $mongoUri = (Get-Content $envFile | Where-Object { $_ -match "^MONGODB_URI=" }) -replace "^MONGODB_URI=", ""
  }
  if (-not $mongoUri) {
    Write-Warning "MONGODB_URI nao encontrado no .env — pulando limpeza automatica."
    Write-Warning "Execute manualmente: docker run --rm mongo:8 mongosh `"<URI>`" --eval `"db.getCollectionNames().forEach(c=>db[c].drop())`""
  } else {
    Write-Host "  [...] Conectando ao MongoDB Atlas via Docker + mongosh..." -ForegroundColor Gray
    docker run --rm mongo:8 mongosh $mongoUri --quiet --eval "
      const cols = db.getCollectionNames();
      cols.forEach(c => { db[c].drop(); print('  dropped: ' + c); });
      print('Total: ' + cols.length + ' colecoes removidas.');
    "
    Write-Host "  [ok]  Banco limpo" -ForegroundColor Green
  }
}

# --- 2. API health check ---
Write-Host ""
Write-Host "=== 2/5  Verificando API ===" -ForegroundColor Cyan
$tries = 0
do {
  try { Invoke-RestMethod -Uri "$ApiUrl/v3/api-docs" | Out-Null; break } catch {}
  $tries++; if ($tries -ge 15) { throw "API nao respondeu. Container rodando?" }
  Write-Host "  [...] Aguardando API... ($tries/15)" -ForegroundColor Gray
  Start-Sleep 2
} while ($true)
Write-Host "  [ok]  API disponivel" -ForegroundColor Green

# --- Dados ---
$donors = @(
  @{name="Ana Silva";cpf="12345678901";birth="1998-05-10";email="ana.silva@blood.local";bt="O-";wt=78.0;phone="22999990001";street="Rua Rocha Leao, 2 - Caju";city="Campos dos Goytacazes";state="RJ";zip="28035-045"},
  @{name="Bruno Santos";cpf="98765432100";birth="1997-08-21";email="bruno.santos@blood.local";bt="O+";wt=82.0;phone="22999990002";street="Rua Barao de Miracema, 140 - Centro";city="Campos dos Goytacazes";state="RJ";zip="28035-562"},
  @{name="Carla Oliveira";cpf="11122233344";birth="1996-04-14";email="carla.oliveira@blood.local";bt="A-";wt=74.0;phone="22999990003";street="Rua Visconde de Itaborai, 402 - Parque Rosario";city="Campos dos Goytacazes";state="RJ";zip="28010-295"},
  @{name="Daniel Costa";cpf="22233344455";birth="1999-09-02";email="daniel.costa@blood.local";bt="A+";wt=68.5;phone="22999990004";street="Avenida Pelinca, 115 - Parque Tamandare";city="Campos dos Goytacazes";state="RJ";zip="28035-053"},
  @{name="Fernanda Lima";cpf="33344455566";birth="1995-12-18";email="fernanda.lima@blood.local";bt="B-";wt=80.0;phone="22999990005";street="Rua Visconde de Itaborai, 427 - Parque Rosario";city="Campos dos Goytacazes";state="RJ";zip="28010-295"},
  @{name="Gabriel Almeida";cpf="44455566677";birth="2000-03-23";email="gabriel.almeida@blood.local";bt="B+";wt=71.5;phone="22999990006";street="Avenida Jose Alves de Azevedo, 337";city="Campos dos Goytacazes";state="RJ";zip="28025-497"},
  @{name="Helena Rocha";cpf="55566677788";birth="1997-07-11";email="helena.rocha@blood.local";bt="AB-";wt=76.0;phone="22999990007";street="Avenida Senador Jose Carlos Pereira Pinto, 400";city="Campos dos Goytacazes";state="RJ";zip="28080-000"},
  @{name="Igor Pereira";cpf="66677788899";birth="1998-11-30";email="igor.pereira@blood.local";bt="AB+";wt=69.0;phone="22999990008";street="Rua Conselheiro Otaviano, 129 - Centro";city="Campos dos Goytacazes";state="RJ";zip="28010-140"},
  @{name="Juliana Martins";cpf="77788899900";birth="2001-01-19";email="juliana.martins@blood.local";bt="O+";wt=77.0;phone="22999990009";street="Rua Barao da Lagoa Dourada, 409 - Centro";city="Campos dos Goytacazes";state="RJ";zip="28035-211"},
  @{name="Lucas Ferreira";cpf="88899900011";birth="2002-06-27";email="lucas.ferreira@blood.local";bt="A+";wt=66.0;phone="22999990010";street="Rua Rocha Leao, 2 - Caju";city="Campos dos Goytacazes";state="RJ";zip="28035-045"}
)

$orgs = @(
  @{name="Hospital Ferreira Machado";cnpj="12345678000100";email="hemo1@blood.local";phone="22988880001";street="Rua Rocha Leao, 2 - Caju";city="Campos dos Goytacazes";state="RJ";zip="28035-045"},
  @{name="Hemocentro Regional de Campos";cnpj="12345678000101";email="hemo2@blood.local";phone="22988880002";street="Rua Rocha Leao, 2 - Caju";city="Campos dos Goytacazes";state="RJ";zip="28035-045"},
  @{name="Hospital Geral Bene Beneficencia Portuguesa";cnpj="12345678000102";email="hemo3@blood.local";phone="22988880003";street="Rua Barao de Miracema, 140 - Centro";city="Campos dos Goytacazes";state="RJ";zip="28035-562"},
  @{name="Nucleo Medicina Transfusional Banco de Sangue";cnpj="12345678000103";email="hemo4@blood.local";phone="22988880004";street="Rua Conselheiro Otaviano, 129 - Centro";city="Campos dos Goytacazes";state="RJ";zip="28010-140"},
  @{name="Santa Casa de Misericordia de Campos";cnpj="12345678000104";email="hemo5@blood.local";phone="22988880005";street="Rua Voluntarios da Patria, 427 - Centro";city="Campos dos Goytacazes";state="RJ";zip="28010-295"},
  @{name="Hospital Unimed Campos";cnpj="12345678000105";email="hemo6@blood.local";phone="22988880006";street="Avenida Pelinca, 115 - Parque Tamandare";city="Campos dos Goytacazes";state="RJ";zip="28035-053"},
  @{name="Hospital dos Plantadores de Cana";cnpj="12345678000106";email="hemo7@blood.local";phone="22988880007";street="Avenida Jose Alves de Azevedo, 337";city="Campos dos Goytacazes";state="RJ";zip="28025-497"},
  @{name="Hospital Geral de Guarus";cnpj="12345678000107";email="hemo8@blood.local";phone="22988880008";street="Avenida Senador Jose Carlos Pereira Pinto, 400";city="Campos dos Goytacazes";state="RJ";zip="28080-000"},
  @{name="Hospital Geral Dr Beda";cnpj="12345678000108";email="hemo9@blood.local";phone="22988880009";street="Rua Conselheiro Otaviano, 129 - Centro";city="Campos dos Goytacazes";state="RJ";zip="28010-140"},
  @{name="Hospital Escola Alvaro Alvim";cnpj="12345678000109";email="hemo10@blood.local";phone="22988880010";street="Rua Barao da Lagoa Dourada, 409 - Centro";city="Campos dos Goytacazes";state="RJ";zip="28035-211"}
)

# --- 3. Registro ---
Write-Host ""
Write-Host "=== 3/5  Registrando doadores e hemocentros ===" -ForegroundColor Cyan

$donorIds = @()
foreach ($d in $donors) {
  $b = "{`"name`":`"$($d.name)`",`"cpf`":`"$($d.cpf)`",`"birthDate`":`"$($d.birth)`",`"email`":`"$($d.email)`",`"password`":`"$Password`",`"passwordConfirmation`":`"$Password`",`"phoneNumber`":`"$($d.phone)`",`"street`":`"$($d.street)`",`"city`":`"$($d.city)`",`"state`":`"$($d.state)`",`"zipCode`":`"$($d.zip)`"}"
  $pid = (Post "/parties/persons" "" $b).id
  $tok = Login $d.email
  Post "/donors" $tok "{`"personId`":`"$pid`",`"bloodType`":`"$($d.bt)`",`"weight`":$($d.wt)}" | Out-Null
  Post "/requesters" $tok "{`"partyId`":`"$pid`"}" | Out-Null
  $donorIds += $pid
  Write-Host "  [ok]  Doador: $($d.name) ($($d.bt))" -ForegroundColor Green
}

$orgIds = @()
foreach ($o in $orgs) {
  $b = "{`"name`":`"$($o.name)`",`"cnpj`":`"$($o.cnpj)`",`"email`":`"$($o.email)`",`"password`":`"$Password`",`"passwordConfirmation`":`"$Password`",`"phoneNumber`":`"$($o.phone)`",`"street`":`"$($o.street)`",`"city`":`"$($o.city)`",`"state`":`"$($o.state)`",`"zipCode`":`"$($o.zip)`"}"
  $oid = (Post "/parties/organizations" "" $b).id
  $tok = Login $o.email
  Post "/requesters" $tok "{`"partyId`":`"$oid`"}" | Out-Null
  Post "/blood-centers" $tok "{`"organizationId`":`"$oid`"}" | Out-Null
  $orgIds += $oid
  Write-Host "  [ok]  Hemocentro: $($o.name)" -ForegroundColor Green
}

# --- 4. Requests ---
Write-Host ""
Write-Host "=== 4/5  Criando pedidos de doacao ===" -ForegroundColor Cyan

$dTypes = "A+","B+","A+","A+","B+","B+","AB+","AB+","O+","A+"
$oTypes = "O+","A+","B+","AB+","O-","A-","B-","AB-","A+","B+"
$dUrg   = "critical","medium","low","critical","medium","low","critical","medium","low","low"
$oUrg   = "medium","critical","low","critical","medium","low","critical","low","medium","critical"
$dGoal  = 2,3,1,4,2,3,1,2,3,1
$oGoal  = 5,10,3,8,6,4,12,5,7,9

for ($i = 0; $i -lt $donors.Count; $i++) {
  $tok = Login $donors[$i].email
  $lim = Days($i + 7)
  $oc  = $i % $orgs.Count
  Post "/donation-requests" $tok "{`"partyId`":`"$($donorIds[$i])`",`"organizationId`":`"$($orgIds[$oc])`",`"bloodTypeNeeded`":`"$($dTypes[$i])`",`"dateLimit`":`"$lim`",`"urgency`":`"$($dUrg[$i])`",`"goalBloodBags`":$($dGoal[$i])}" | Out-Null
  Write-Host "  [ok]  Request doador $($i+1): $($dTypes[$i]) / $($dUrg[$i]) / limite $lim" -ForegroundColor Green
}

for ($i = 0; $i -lt $orgs.Count; $i++) {
  $tok = Login $orgs[$i].email
  $lim = Days($i + 10)
  Post "/donation-requests" $tok "{`"partyId`":`"$($orgIds[$i])`",`"organizationId`":`"$($orgIds[$i])`",`"bloodTypeNeeded`":`"$($oTypes[$i])`",`"dateLimit`":`"$lim`",`"urgency`":`"$($oUrg[$i])`",`"goalBloodBags`":$($oGoal[$i])}" | Out-Null
  Write-Host "  [ok]  Request hemocentro $($i+1): $($oTypes[$i]) / $($oUrg[$i]) / limite $lim" -ForegroundColor Green
}

# --- 5. Doacoes ---
Write-Host ""
Write-Host "=== 5/5  Registrando doacoes concluidas (hoje: $Today) ===" -ForegroundColor Cyan

$scenarios = @((0,0),(1,0),(8,0),(1,1),(3,1),(8,1),(2,2),(6,2),(0,3),(4,3))
$n = 0
foreach ($s in $scenarios) {
  $di = $s[0]; $oi = $s[1]
  $tok = Login $donors[$di].email
  Post "/donations" $tok "{`"personId`":`"$($donorIds[$di])`",`"organizationId`":`"$($orgIds[$oi])`",`"donationDate`":`"$Today`"}" | Out-Null
  $n++
  Write-Host "  [ok]  Doacao $n`: $($donors[$di].email) -> $($orgs[$oi].email)" -ForegroundColor Green
}

# --- Resumo ---
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Reseed concluido com sucesso!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Data de referencia : $Today"
Write-Host "  Doadores           : $($donors.Count)"
Write-Host "  Hemocentros        : $($orgs.Count)"
Write-Host "  Requests criadas   : $($donors.Count + $orgs.Count)"
Write-Host "  Doacoes de hoje    : $n"
Write-Host "  Senha padrao       : $Password"
Write-Host ""
Write-Host "Logins de doadores:" -ForegroundColor Cyan
$donors | ForEach-Object { Write-Host "  $($_.email)   tipo: $($_.bt)" }
Write-Host ""
Write-Host "Logins de hemocentros:" -ForegroundColor Cyan
$orgs   | ForEach-Object { Write-Host "  $($_.email)" }
Write-Host ""
Write-Host "Criterios de teste:" -ForegroundColor Cyan
Write-Host "  hemo1-hemo4  -> tem doacoes hoje (metas atingidas/parciais)"
Write-Host "  hemo5-hemo10 -> sem doacoes (requests abertas sem progresso)"
Write-Host "  Gabriel, Igor, Lucas -> elegiveis para recomendacoes"