# Fulfillment e concorrência

O preenchimento **não é persistido**. Cada leitura monta um snapshot em memória (`DonationRequestFulfillmentService.fill`).

Não há write path de contador, `@Version` de fulfillment, lock por hemocentro nem retry. Completar uma doação só grava a doação; o próximo GET recalcula.

A simulação antiga (`lost_refresh`, lock in-process) deixou de se aplicar a esse modelo.
