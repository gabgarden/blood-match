# Roteiro e Guia de Apresentação Técnica para o Orientador

Este documento foi preparado especialmente para orientar a apresentação do projeto **Blood Match** ao orientador. Ele reúne a fundamentação teórica, as decisões de arquitetura, o funcionamento dos algoritmos e as respostas estratégicas para perguntas de validação acadêmica.

---

## 📋 Ficha Técnica do Projeto

| Item | Descrição / Escolha Técnica |
|---|---|
| **Sistema** | **Blood Match** - Plataforma Inteligente de Gestão e Recomendação de Doação de Sangue |
| **Estilo Arquitetural** | **Clean Architecture** + **Domain-Driven Design (DDD)** |
| **Padrão de Pacotes** | **Package-by-Feature / Aggregates** (*interfaces de repositório junto das entidades*) |
| **Estratégia de Performance** | **Contador Materializado na Escrita (Write-Path Refresh)** |
| **Status dos Testes** | **106 Testes Automatizados Passando** (`BUILD SUCCESS`) |

---

## 🎙️ Roteiro Sequencial para a Apresentação

### Passo 1: Contextualização da Arquitetura (DDD & Package-by-Feature)
> *"Professor, organizamos o backend seguindo rigorosamente os princípios de Clean Architecture e Domain-Driven Design (DDD). Por motivos de alta coesão e modularização, aplicamos a organização por Agregados (Package-by-Feature): as interfaces de repositório de domínio agora residem exatamente no mesmo pacote que suas entidades principais."*

* **Onde mostrar no código:**
  - `bloodmatch.domain.donation`: Contém [Donation.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/donation/Donation.java) e [DonationRepositoryInterface.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/donation/DonationRepositoryInterface.java).
  - `bloodmatch.domain.donationrequest`: Contém [DonationRequest.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/donationrequest/DonationRequest.java) e [DonationRequestRepositoryInterface.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/donationrequest/DonationRequestRepositoryInterface.java).
  - `bloodmatch.domain.roles.person.donor`: Contém [Donor.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/roles/person/donor/Donor.java) e [DonorRepositoryInterface.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/roles/person/donor/DonorRepositoryInterface.java).

---

### Passo 2: O Desafio de Negócio – "O Conceito de Pool por Hemocentro"
> *"Diferente de sistemas onde o doador vincula sua doação a um único pedido (relação 1:1), no Blood Match as doações completadas em um hospital entram para um **pool de doações** daquele hemocentro. Essas doações atendem dinamicamente aos pedidos de doação ativos do hospital."*

* **Conceito Chave**:
  - Não há chave estrangeira `donation.donation_request_id`.
  - O número de bolsas preenchidas (`fulfilledBloodBags`) de um pedido é o resultado da simulação determinística desse pool.

---

### Passo 3: Decisão de Engenharia – Contador Materializado (*Read Path vs Write Path*)
> *"Para garantir que a listagem de pedidos e recomendações no app seja ultra-rápida, adotamos o padrão de **Contador Materializado (Denormalized Counter)**:"*

* **Read Path (Caminho de Leitura Frequente)**:
  - As APIs de consulta (ex: [GetRecommendedRequestsUseCase.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/application/usecase/donationrequest/recommendations/GetRecommendedRequestsUseCase.java)) fazem leitura direta do campo `fulfilledBloodBags` no documento MongoDB ($O(1)$). **Zero custo de recálculo**.
* **Write Path (Caminho de Escrita Frio)**:
  - O recálculo é disparado somente quando uma doação é concluída ([CreateCompletedDonationUseCase.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/application/usecase/donation/createcompleted/CreateCompletedDonationUseCase.java) ou [CompletePendingDonationUseCase.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/application/usecase/donation/completependingdonation/CompletePendingDonationUseCase.java)).
  - O [DonationRequestFulfillmentRefresher.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/application/usecase/donation/fulfillment/DonationRequestFulfillmentRefresher.java) orquestra a busca das doações e pedidos do hemocentro, executa o cálculo em memória e atualiza a persistência.

---

### Passo 4: O Algoritmo de Alocação (FIFO + Compatibilidade Sanguínea)
> *"O cálculo de alocação vive de forma pura no domínio, no serviço [DonationRequestFulfillmentService.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/services/DonationRequestFulfillmentService.java)."*

* **Regras do Algoritmo**:
  1. **Prioridade FIFO**: Ordena os pedidos ativos por data de solicitação (`dateRequested ASC`) e as doações completadas por data de doação (`donationDate ASC`). Pedidos mais antigos recebem doações primeiro.
  2. **Compatibilidade ABO/Rh**: Validação matemática em [BloodType.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/shared/valueObjects/BloodType.java) (ex: `O-` doa para todos, `A+` para `A+` e `AB+`).
  3. **Janela Temporal**: A doação só atende ao pedido se a data da doação estiver entre `dateRequested` e `dateLimit` ([DonationRequest.acceptsDonation](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/donationrequest/DonationRequest.java#L328)).

---

## 🛡️ Guia de Defesa: Perguntas Prováveis do Orientador

### ❓ Pergunta 1: "Por que você não faz apenas um incremento (`+1`) no pedido quando o doador faz a doação?"
> **Resposta Estratégica**:
> 1. **Redistribuição por Expiração/Cancelamento**: Pedidos têm validade (`dateLimit`). Quando um pedido expira ou é cancelado, as bolsas do pool que antes o atendiam precisam ser **liberadas e redistribuídas** para os outros pedidos ativos do hospital. Com um incremento `+1` fixo, essas bolsas ficariam "presas" no histórico.
> 2. **Idempotência e Tolerância a Falhas**: Se a aplicação cair ou houver uma retentativa de requisição, um operador `+1` poderia duplicar a contagem. O recálculo determinístico garante que o estado do banco seja sempre 100% correto.
> 3. **Novas Requests**: Quando uma nova request é criada, doações válidas do pool do hemocentro passam a ser consideradas no próximo refresh.

---

### ❓ Pergunta 2: "Essa abordagem de recalcular o pool na escrita escala se o hospital tiver milhares de doações?"
> **Resposta Estratégica**:
> *"Sim, para o perfil do produto atual. As leituras (doadores buscando onde doar) ocorrem centenas de vezes mais que a conclusão de doações. Mover o custo para o caminho de escrita (write path) é o trade-off correto. Além disso, o recálculo é filtrado **apenas por hemocentro** (`organizationId`), isolando o impacto."*

---

### ❓ Pergunta 3: "Onde está a regra que impede um doador de doar se ele não respeitar o intervalo mínimo de doação?"
> **Resposta Estratégica**:
> *"Está no método `Donor.canDonateOn(date)` em [Donor.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/roles/person/donor/Donor.java). Ele valida os prazos regulamentares de doação de sangue por sexo e intervalo de dias."*

---

## 🗺️ Mapa de Navegação Rápida no Código (Cheat Sheet)

Se o orientador pedir para abrir arquivos específicos durante a reunião:

| Funcionalidade / Camada | Arquivo Principal |
|---|---|
| **Serviço de Alocação FIFO** | [DonationRequestFulfillmentService.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/services/DonationRequestFulfillmentService.java) |
| **Atualizador na Escrita (Write Refresher)** | [DonationRequestFulfillmentRefresher.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/application/usecase/donation/fulfillment/DonationRequestFulfillmentRefresher.java) |
| **Regra de Aceite e Elegibilidade do Pedido** | [DonationRequest.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/donationrequest/DonationRequest.java#L328) |
| **Matriz de Compatibilidade Sanguínea** | [BloodType.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/domain/shared/valueObjects/BloodType.java#L45) |
| **Caso de Uso: Criar Doação Concluída** | [CreateCompletedDonationUseCase.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/application/usecase/donation/createcompleted/CreateCompletedDonationUseCase.java) |
| **Caso de Uso: Recomendações (Leitura)** | [GetRecommendedRequestsUseCase.java](file:///c:/Users/garde/Desktop/projects/blood-match/backend/src/main/java/bloodmatch/application/usecase/donationrequest/recommendations/GetRecommendedRequestsUseCase.java) |
| **Documentação Técnica de Fulfillment** | [LOGICA_PREENCHIMENTO_REQUESTS.md](file:///c:/Users/garde/Desktop/projects/blood-match/docs/LOGICA_PREENCHIMENTO_REQUESTS.md) |

---

## 📈 Status de Validação do Sistema

```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 106, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```
- **106 testes automatizados executados e 100% aprovados.**
