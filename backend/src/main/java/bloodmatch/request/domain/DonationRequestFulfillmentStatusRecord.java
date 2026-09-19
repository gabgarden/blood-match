package bloodmatch.request.domain;

import java.math.BigDecimal;

public record DonationRequestFulfillmentStatusRecord(
    BigDecimal fulfilledBloodBags,
    boolean goalReached
) {
  public DonationRequestFulfillmentStatusRecord(int fulfilledBloodBags, boolean goalReached) {
    this(BigDecimal.valueOf(fulfilledBloodBags), goalReached);
  }
}