protected boolean hasValidAge(LocalDate currentDate) {
    int age = getPerson().getAge(currentDate);
    return age >= 16 && age <= 69;
}

public boolean isEligibleToDonate(LocalDate currentDate) {
    if (!hasValidAge(currentDate)) {
        return false;
    }
    if (lastDonationDate == null) {
        return true;
    }
    return !lastDonationDate
        .plusMonths(DonationPolicy.getDonationIntervalInMonths())
        .isAfter(currentDate);
}
