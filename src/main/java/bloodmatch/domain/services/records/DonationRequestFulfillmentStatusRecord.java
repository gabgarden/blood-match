package bloodmatch.domain.services.records;

public record DonationRequestFulfillmentStatusRecord(
    int fulfilledBloodBags,
    boolean goalReached
) {
}