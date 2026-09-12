# Lógica de preenchimento de pedidos (`DonationRequest`)

O progresso de bolsas **não é persistido**. É um **snapshot em memória**, calculado quando a API precisa **mostrar** as requests.

Não há vínculo doação → request. Cada doação `COMPLETED` de um hemocentro entra num pool e é distribuída FIFO + ABO/Rh + janela de datas.

Não há concorrência nesse cálculo: leitura de requests e doações, alocação em memória, resposta. Nada é regravado.



---

## Domain service

Classe: `DonationRequestFulfillmentService`.

```text
fill(bloodCenter, asOfDate)

1. carrega as solicitações ativas daquele hemocentro
2. o intervalo de doações começa na solicitação ativa mais antiga
3. carrega as doações COMPLETED daquele hemocentro no intervalo
4. ordena solicitações e doações da mais antiga para a mais nova
5. 1ª passagem: cada doação vai para a primeira solicitação que acceptsDonation e
   ainda não bateu o TETO PROPORCIONAL do dia
6. 2ª passagem: as doações que sobraram repetem o FIFO, agora até a meta cheia
7. devolve requestId → (fulfilledBloodBags, goalReached)
```

`fill(requests, asOfDate)` faz o mesmo, carregando as solicitações **ativas** de todos os hemocentros da lista (não o histórico expirado). É o caminho das recomendações e da lista do requisitante.

`acceptsDonation` exige: mesmo hemocentro, request ativa, não expirada na data do snapshot, doação completa, `dateRequested ≤ donationDate ≤ dateLimit`, tipo sanguíneo compatível.

---

## Teto proporcional

`DonationRequest.proportionalGoalAt(asOfDate)` libera a meta no mesmo ritmo em que a janela `[dateRequested, dateLimit]` é consumida:

```text
teto = goalBloodBags × (asOfDate - dateRequested) / (dateLimit - dateRequested)
```

- calculada com precisão contínua (`BigDecimal`, escala 4)
- criada hoje → teto 0 (nada da janela passou)
- no dia do limite → teto = meta cheia
- janela de um único dia → teto = meta cheia (não há o que escalonar)
- na alocação FIFO, a checagem `given < limit` permite que frações decimais (ex.: 0,1 ou 2,28 bolsas) absorvam a primeira bolsa sem necessitar de arredondamentos inteiros artificiais

O objetivo é racionar em favor de quem está perto de expirar. Como a segunda passagem devolve as sobras, o total alocado não muda — com pool abundante o resultado é idêntico ao FIFO puro.

Efeito colateral aceito: como nada é persistido e os tetos crescem a cada dia, o progresso exibido de uma request de prazo folgado pode **regredir** entre dois snapshots, quando as bolsas migram para uma irmã de prazo apertado.

---

## Onde o snapshot é usado (leitura)

| Caso de uso | Papel |
|---|---|
| `GetDonationRequestsByPartyIdUseCase` | lista do requisitante com progresso |
| `GetRecommendedRequestsUseCase` | recomendações; omite request com meta atingida |
| `NotifyPotentialDonorsUseCase` | bloqueia notificação se a meta já foi atingida |

Criar ou completar doação **só persiste a doação**. O próximo GET já vê o pool novo.

---

## Exemplo

Hemocentro Central:

- R1 em 10/01, precisa `A+`, meta 2
- R2 em 12/01, precisa `O+`, meta 1
- D1 em 11/01, `O-` → R1 (1/2)
- D2 em 13/01, `A+` → R1 (2/2)
- D3 em 14/01, `O+` → R2 (1/1)

## Exemplo com teto (escassez)

Snapshot em 24/07, hemocentro Central, 8 doações `O-` no pool:

- R1 pedida em 14/07, limite 24/08, meta 10 → janela 41 dias, 10 decorridos, teto 3
- R2 pedida em 20/07, limite 27/07, meta 4 → janela 7 dias, 4 decorridos, teto 3

1ª passagem: 3 para R1, 3 para R2, sobram 2. 2ª passagem: as 2 voltam para R1, a mais antiga com espaço.

Resultado: R1 5/10, R2 3/4. No FIFO puro seria R1 8/10 e R2 0/4 — R2 expiraria em 3 dias zerada.
