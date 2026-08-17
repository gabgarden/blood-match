# Preenchimento de Donation Requests (snapshot)

Cada request tem uma meta (`goalBloodBags`). Quantas bolsas já contam (`fulfilledBloodBags`) **não fica no Mongo**. É o resultado de um snapshot: pool de doações `COMPLETED` do hemocentro, distribuído FIFO + compatibilidade + janela de datas.

## Domain service

`DonationRequestFulfillmentService.fill(bloodCenter, startDate, endDate)`:

1. Consulta todas as requests daquele hemocentro.
2. Consulta as doações concluídas daquele hemocentro no intervalo.
3. Aloca em memória (FIFO).
4. Devolve o mapa de progresso. Não grava nada.

Não há lock, refresh nem campo desnormalizado. Leituras concorrentes só leem o banco; cada uma monta o próprio snapshot.

## Onde aparece

Na listagem de requests do requisitante, nas recomendações ao doador e no bloqueio de notificação quando a meta já foi atingida.
