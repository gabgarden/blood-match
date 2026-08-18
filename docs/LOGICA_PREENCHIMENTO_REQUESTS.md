# Lógica de preenchimento de pedidos (`DonationRequest`)

O progresso de bolsas **não é persistido**. Ele é um **snapshot em memória** do banco, calculado quando a API precisa **mostrar** as requests.

Não há vínculo doação → request. Cada doação `COMPLETED` de um hemocentro entra num pool e é distribuída FIFO + ABO/Rh + janela de datas.

Não há concorrência nesse cálculo: leitura de requests e doações, alocação em memória, resposta. Nada é regravado.

---

## Domain service

Classe: `DonationRequestFulfillmentService`.

```text
fill(bloodCenter, startDate, endDate)

1. carrega todas as solicitações daquele hemocentro
2. o intervalo de doações começa na solicitação mais antiga, se ela for anterior a startDate
3. carrega as doações COMPLETED daquele hemocentro no intervalo
4. ordena solicitações e doações da mais antiga para a mais nova
5. cada doação vai para a primeira solicitação que acceptsDonation e ainda não bateu a meta
6. devolve requestId → (fulfilledBloodBags, goalReached)
```

`acceptsDonation` exige: mesmo hemocentro, request ativa, não expirada na data do snapshot, doação completa, `dateRequested ≤ donationDate ≤ dateLimit`, tipo sanguíneo compatível.

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
