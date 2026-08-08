# Preenchimento de Donation Requests (`fulfilledBloodBags`)

Documento para quem está começando no projeto: explica **o que** o sistema calcula, **quando** calcula, **por que** mudamos a estratégia e se isso é uma boa otimização.

---

## 1. O problema em uma frase

Cada *donation request* tem uma meta (`goalBloodBags`) e precisa mostrar quantas bolsas já “contam” para essa meta (`fulfilledBloodBags`).

O detalhe importante: **não existe vínculo direto doação → request**.  
Uma doação completada em um hemocentro entra em um **pool** e é **distribuída** entre as requests ativas daquele hemocentro, seguindo regras (FIFO + compatibilidade de sangue + janela de datas).

Por isso o número `fulfilledBloodBags` **não é um simples `COUNT` no banco**. Ele é o resultado de um algoritmo de distribuição.

---

## 2. Regra de negócio (como a distribuição funciona)

Classe principal: `DonationRequestFulfillmentService`.

### Ideia mental

Imagine um hemocentro com várias requests abertas e várias doações já completadas.

1. Ordena as **requests** da mais antiga para a mais nova (`dateRequested`, depois `id`).
2. Ordena as **doações** da mais antiga para a mais nova (`donationDate`, depois `id`).
3. Para **cada doação**, percorre as requests daquele hemocentro e tenta “encaixar” a doação na **primeira** request que:
   - está ativa e não expirada (no cálculo atual);
   - aceita o tipo sanguíneo do doador;
   - tem a data da doação entre `dateRequested` e `dateLimit`;
   - ainda não atingiu a meta (`fulfilled < goal`).
4. Cada doação só preenche **uma** request (consome 1 bolsa e para).

Isso é **FIFO por hemocentro**: a request mais antiga compatível “come” a doação antes das mais novas.

```text
Hemocentro X
  Requests (antigas → novas):  R1 (meta 2)   R2 (meta 1)
  Doações (antigas → novas):   D1, D2, D3

  D1 → R1  (R1 fica 1/2)
  D2 → R1  (R1 fica 2/2, meta atingida)
  D3 → R2  (R2 fica 1/1)
```

A checagem “essa doação serve para essa request?” fica em `DonationRequest.acceptsDonation(...)`.

---

## 3. Estratégia antiga vs estratégia atual

### Antes (calcular na leitura)

Toda vez que íamos **mostrar** requests (recomendações, lista por party, etc.):

1. Buscava as requests.
2. Buscava doações completadas.
3. Rodava o algoritmo em memória.
4. Só então devolveva `fulfilledBloodBags` na resposta.

**Problema:** leituras são frequentes. Recalcular o pool em toda listagem escala mal (custo alto e repetido).

### Agora (materializar na escrita)

`fulfilledBloodBags` é um **campo persistido** na request (Mongo: `DonationRequestSchema.fulfilledBloodBags`).

- Nas **leituras**, só lemos o campo.
- Nas **escritas que mudam o pool de doações completadas**, recalculamos e salvamos o contador.

Isso se chama *materialized / denormalized counter*: guardamos o resultado do cálculo para leituras rápidas.

---

## 4. Onde o contador é atualizado (write path)

Hoje o refresh é disparado por **dois** use cases, mas o write path vive em um só lugar:

| Use case | Quando |
|----------|--------|
| `CreateCompletedDonationUseCase` | Doação já registrada como completa |
| `CompletePendingDonationUseCase` | Doação pendente é marcada como completa |

Ambos chamam `DonationRequestFulfillmentRefresher.refresh(organizationId, currentDate)`, onde `organizationId` é o party id da organization do hemocentro (campo Mongo `organizationId` em requests e donations).

Fluxo comum (`DonationRequestFulfillmentRefresher`):

```text
1. Salva a doação (e o donor, se preciso) — no use case
2. Busca requests ATIVAS da organization (ainda não expiradas)
3. Busca TODAS as doações COMPLETADAS daquela organization (ordenadas)
4. Chama fulfillmentService.synchronize(requests, donations, currentDate)
5. Para cada request, donationRequestRepository.save(request)
```

`synchronize` faz duas coisas:

1. `calculate(...)` — roda o algoritmo FIFO e monta o mapa de status.
2. Atualiza `request.setFulfilledBloodBags(...)` em memória.

Depois o refresher **persiste** cada request com o novo valor.

### O que NÃO dispara refresh (de propósito)

| Ação | Atualiza `fulfilledBloodBags`? |
|------|--------------------------------|
| Criar doação **pendente** | Não — pendente ainda não conta |
| Criar request | Não — começa em `0` |
| Cancelar / fechar request | Não |
| Alterar `goalBloodBags` | Não |
| Alterar `dateLimit` | Não |
| Listar recomendações | Não — só lê |
| Listar requests por party | Não — só lê |

---

## 5. Onde o contador é apenas lido (read path)

### Recomendações — `GetRecommendedRequestsUseCase`

- Busca candidates no banco.
- Filtra com `!request.isGoalReached()` (usa o campo persistido).
- Resposta usa `request.getFulfilledBloodBags()`.

### Requests do usuário — `GetDonationRequestsByPartyIdUseCase`

- Busca requests do requester.
- Lê `getFulfilledBloodBags()` e calcula `remaining = goal - fulfilled`.

**Nenhum dos dois** carrega doações nem chama `DonationRequestFulfillmentService`.

---

## 6. Diagrama do fluxo atual

```text
                    ┌─────────────────────────────┐
                    │  Doação completa criada     │
                    │  ou pendente completada     │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │ DonationRequestFulfillment  │
                    │ Refresher.refresh           │
                    │ (só daquela organization)   │
                    └──────────────┬──────────────┘
                                   │
              ┌────────────────────┼────────────────────┐
              ▼                                         ▼
   requests ativas                          doações completadas
   (organizationId)                         (organizationId)
              │                                         │
              └────────────────────┬────────────────────┘
                                   ▼
                    ┌─────────────────────────────┐
                    │ DonationRequestFulfillment  │
                    │ Service.synchronize         │
                    │ (FIFO + compatibilidade)    │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
                    ┌─────────────────────────────┐
                    │ Persiste fulfilledBloodBags │
                    │ em cada request             │
                    └──────────────┬──────────────┘
                                   │
                                   ▼
         ┌─────────────────────────────────────────────┐
         │ LEITURAS (recomendações / lista por party)  │
         │ só leem o campo — sem recalcular            │
         └─────────────────────────────────────────────┘
```

---

## 7. Avaliação: essa é uma boa estratégia de otimização?

### Veredito curto

**Sim, para o perfil do produto atual.**  
Leituras (recomendações e listagens) tendem a ser bem mais frequentes que “completar doação”. Mover o custo do caminho quente (read) para o caminho frio (write) é a troca certa.

### Por que faz sentido

| Critério | Avaliação |
|----------|-----------|
| Leituras rápidas | Ótimo — campo já pronto |
| Escopo do recálculo | Bom — só o hemocentro afetado, não o sistema inteiro |
| Corretude do FIFO | Boa — o algoritmo completo ainda roda no write (não é um `+1` ingênuo) |
| Complexidade | Aceitável — um serviço de domínio + refresh em 2 use cases |
| Testabilidade | Boa — há testes de FIFO e de persistência após mutação |

### Por que **não** usar só `incrementFulfilledBloodBags()` no write

Incrementar `+1` na “primeira request compatível” na hora da doação parece mais barato, mas:

- perde a garantia de **redistribuição global** do pool;
- fica frágil se regras mudarem (meta, prazo, cancelamento, nova request);
- o sistema já modela o problema como **recalcular a alocação**, não como “apontar a doação para uma request”.

Recalcular o pool no write e materializar o resultado é o meio-termo certo entre corretude e performance.

### Custo do write path (honesto)

A cada doação completada no hemocentro:

- carrega requests ativas;
- carrega **histórico** de doações completadas daquele hemocentro;
- roda algo próximo de `O(doações × requests)` naquele hemocentro;
- salva N requests.

Para volume típico de um hemocentro, isso costuma ser aceitável.  
Se no futuro um hemocentro tiver milhares de doações históricas e o write ficar lento, aí sim vale evoluir (ex.: janela de datas, projeção incremental, job assíncrono). **Hoje isso seria prematuro.**

---

## 8. Pontos de atenção (importante para o junior)

Materializar na escrita traz um trade-off clássico: **leituras rápidas × possível atraso de consistência** até o próximo refresh.

Hoje o refresh **só** roda quando uma doação completa entra no sistema. Então o contador pode ficar “atrasado” nestes casos:

1. **Nova request** criada num hemocentro que já tem doações “sobrando” no pool  
   → começa em `0` até a próxima doação completa disparar o recálculo.

2. **Cancelar / fechar** uma request  
   → as “bolsas” que o algoritmo dava a ela só são redistribuídas às irmãs no próximo refresh.

3. **Mudar `dateLimit` ou `goalBloodBags`**  
   → muda quem pode receber doações / se a meta já foi batida, mas o campo persistido só é reescrito no próximo refresh.

4. **Request expirar**  
   → some da lista usada no refresh (`findActiveRequestsByOrganizationIds`). Doações históricas passam a ser redistribuídas só entre as ainda ativas (respeitando a janela de datas de cada uma). O valor antigo na request expirada permanece como histórico da última sincronização.

Isso **não invalida** a estratégia de otimização; só define o contrato:  
*“`fulfilledBloodBags` está correto em relação ao último recálculo disparado por doação completa.”*

Se o produto precisar de consistência imediata após criar/cancelar/alterar request, o caminho natural é **também** chamar o mesmo `DonationRequestFulfillmentRefresher` nesses use cases (ainda sem recalcular nas leituras).

---

## 9. Comparação rápida das estratégias possíveis

| Estratégia | Leituras | Escritas | Corretude FIFO | Quando usar |
|------------|----------|----------|----------------|-------------|
| Calcular sempre na leitura (antiga) | Caras | Baratas | Sempre fresca | Poucas leituras |
| **Materializar no write de doação (atual)** | Baratas | Médias | Fresca após doação | **Nosso caso** |
| Materializar em todo evento que muda elegibilidade | Baratas | Um pouco mais | Quase sempre fresca | Se stale após create/cancel/update incomodar |
| Só `+1` incremental | Baratas | Muito baratas | Frágil | Evitar com o modelo de pool atual |

---

## 10. Mapa de arquivos (para navegar no código)

| Papel | Arquivo |
|-------|---------|
| Algoritmo FIFO | `domain/services/DonationRequestFulfillmentService.java` |
| Status calculado | `domain/services/records/DonationRequestFulfillmentStatusRecord.java` |
| Campo + `acceptsDonation` | `domain/donationrequest/DonationRequest.java` |
| Persistência do contador | `infra/persistence/schema/DonationRequestSchema.java` |
| Write path compartilhado | `application/.../fulfillment/DonationRequestFulfillmentRefresher.java` |
| Write: doação completa | `application/.../createcompleted/CreateCompletedDonationUseCase.java` |
| Write: completar pendente | `application/.../completependingdonation/CompletePendingDonationUseCase.java` |
| Read: recomendações | `application/.../recommendations/GetRecommendedRequestsUseCase.java` |
| Read: requests por party | `application/.../GetDonationRequestsByPartyIdUseCase.java` |

---

## 11. Resumo para lembrar

1. `fulfilledBloodBags` **não** é contagem direta; é **alocação FIFO** de doações do hemocentro.
2. Calculamos na **mutação de doação completa** e **persistimos**.
3. Nas **telas / listagens**, só **lemos** o valor.
4. Essa é a estratégia certa quando há **muitas leituras** e **poucas conclusões de doação**.
5. Se no futuro o contador precisar refletir na hora create/cancel/update de request, **estenda o refresh para esses writes** — não volte a calcular nas leituras.
