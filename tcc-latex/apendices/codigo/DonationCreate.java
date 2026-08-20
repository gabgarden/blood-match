public static Donation create(
        Donor donor,
        BloodCenter bloodCenter,
        LocalDate intendedDate,
        LocalDate donationDate,
        LocalTime expectedTime,
        LocalDate currentDate) {
    boolean hasIntendedDate = intendedDate != null;
    boolean hasDonationDate = donationDate != null;
    if (hasIntendedDate == hasDonationDate) {
        throw new IllegalArgumentException("Provide either intendedDate or donationDate");
    }
    if (hasIntendedDate && intendedDate.isBefore(currentDate)) {
        throw new IllegalArgumentException("Intended date cannot be in the past");
    }
    if (hasDonationDate && donationDate.isAfter(currentDate)) {
        throw new IllegalArgumentException("Donation date cannot be in the future");
    }
    return new Donation(donor, bloodCenter, intendedDate, donationDate, expectedTime);
}

public boolean isPending() {
    return donationDate == null && cancelledAt == null;
}

public boolean isCompleted() {
    return donationDate != null;
}

public boolean isCancelled() {
    return cancelledAt != null;
}
