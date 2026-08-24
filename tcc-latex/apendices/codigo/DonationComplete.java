private static void validateDates(
        LocalDate intendedDate,
        LocalDate donationDate,
        LocalDate cancelledAt) {
    if (donationDate != null && cancelledAt != null) {
        throw new IllegalArgumentException("Donation cannot be completed and cancelled");
    }
    if (cancelledAt != null && intendedDate == null) {
        throw new IllegalArgumentException("Cancelled donation requires intendedDate");
    }
    if (donationDate == null && cancelledAt == null && intendedDate == null) {
        throw new IllegalArgumentException("Donation requires intendedDate or donationDate");
    }
}

public void complete(LocalDate completionDate, LocalDate currentDate) {
    if (completionDate == null)
        throw new IllegalArgumentException("Completion date cannot be null");
    if (currentDate == null)
        throw new IllegalArgumentException("Current date cannot be null");
    if (!isPending())
        throw new IllegalStateException("Only pending donations can be completed");
    if (completionDate.isAfter(currentDate))
        throw new IllegalArgumentException("Completion date cannot be in the future");

    this.donationDate = completionDate;
}
