public boolean acceptsDonation(Donation donation, LocalDate currentDate) {
    if (donation == null)
        throw new IllegalArgumentException("Donation cannot be null");
    if (currentDate == null)
        throw new IllegalArgumentException("Current date cannot be null");

    if (!isActive())
        return false;
    if (isExpired(currentDate))
        return false;
    if (!donation.isCompleted())
        return false;
    if (donation.getDonationDate().isBefore(dateRequested))
        return false;
    if (donation.getDonationDate().isAfter(dateLimit))
        return false;

    return donation.getDonor()
            .getBloodType()
            .canDonateTo(bloodTypeNeeded);
}
