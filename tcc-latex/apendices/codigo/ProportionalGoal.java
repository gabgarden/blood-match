public int proportionalGoalAt(LocalDate asOfDate) {
    if (asOfDate == null)
        throw new IllegalArgumentException("As of date cannot be null");

    long windowDays = ChronoUnit.DAYS.between(dateRequested, dateLimit);

    // janela de um unico dia: nao ha tempo a escalonar, a meta vale inteira
    if (windowDays <= 0)
        return goalBloodBags;

    long elapsedDays = ChronoUnit.DAYS.between(dateRequested, asOfDate);

    // ainda no dia do pedido: nenhuma fracao da janela foi consumida
    if (elapsedDays <= 0)
        return 0;

    // no dia do limite o teto deixa de existir: a meta e liberada inteira
    if (elapsedDays >= windowDays)
        return goalBloodBags;

    // ceil(goal * elapsed / window) em aritmetica inteira
    return (int) ((goalBloodBags * elapsedDays + windowDays - 1) / windowDays);
}
