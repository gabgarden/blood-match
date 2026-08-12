private static void validateStatusFlags(
        boolean isCompleted,
        boolean isPending,
        boolean isCancelled) {
    int activeStates = 0;
    if (isCompleted) activeStates++;
    if (isPending) activeStates++;
    if (isCancelled) activeStates++;

    if (activeStates != 1) {
        throw new IllegalArgumentException(
            "Donation status must be exactly one of completed, pending or cancelled");
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
    this.completed = true;
    this.pending = false;
}
