# Preenchimento de Donation Requests (snapshot)

Cada request persiste só a meta (`goalBloodBags`). `fulfilledBloodBags` / `goalReached` **não** ficam no Mongo. São um snapshot: pool de doações `COMPLETED` do hemocentro, distribuído FIFO + ABO/Rh + janela de datas + teto proporcional.

Não há lock, campo desnormalizado nem recálculo na escrita. Leituras concorrentes só leem o banco; cada uma monta o próprio snapshot.

## Domain service

`DonationRequestFulfillmentService`:

- `fill(bloodCenter, asOfDate)` — pedidos **ativos** daquele hemocentro
- `fill(requests, asOfDate)` — recarrega ativos dos hemocentros da lista (recomendações e listagem do requisitante)

O intervalo de doações começa na solicitação ativa mais antiga e vai até `asOfDate`. Alocação em memória. Nada é gravado.

## Teto proporcional

A meta não é liberada de uma vez: cada request só recebe, na primeira passagem, `ceil(goal × dias decorridos / dias da janela)` — `DonationRequest.proportionalGoalAt`. Passados 3 de 10 dias, só 3/10 da meta. Assim uma request de prazo folgado não esvazia o pool antes de uma irmã perto de expirar.

As doações que ninguém pôde absorver voltam numa **segunda passagem FIFO**, agora limitada pela meta cheia. O total alocado é o mesmo do FIFO puro; o teto só muda **quem** recebe sob escassez.

## Onde aparece

- listagem do requisitante
- recomendações ao doador (omite meta já atingida)
- notificação (bloqueia se a meta já foi atingida)

Passo a passo e exemplo: [`LOGICA_PREENCHIMENTO_REQUESTS.md`](LOGICA_PREENCHIMENTO_REQUESTS.md).
