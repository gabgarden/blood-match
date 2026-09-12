public BigDecimal proportionalGoalAt(LocalDate asOfDate) {
    if (asOfDate == null)
        throw new IllegalArgumentException("As of date cannot be null");

    long windowDays = ChronoUnit.DAYS.between(dateRequested, dateLimit);

    // janela de um unico dia: nao ha tempo a escalonar, a meta vale inteira
    if (windowDays <= 0)
        return BigDecimal.valueOf(goalBloodBags);

    long elapsedDays = ChronoUnit.DAYS.between(dateRequested, asOfDate);

    // ainda no dia do pedido: nenhuma fracao da janela foi consumida
    if (elapsedDays <= 0)
        return BigDecimal.ZERO;

    // no dia do limite o teto deixa de existir: a meta e liberada inteira
    if (elapsedDays >= windowDays)
        return BigDecimal.valueOf(goalBloodBags);

    return BigDecimal.valueOf(goalBloodBags)
            .multiply(BigDecimal.valueOf(elapsedDays))
            .divide(BigDecimal.valueOf(windowDays), 4, RoundingMode.HALF_UP);
}
