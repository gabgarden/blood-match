package bloodmatch.interfaces.rest.donationrequest.acceptandcreatepending;

import java.time.LocalDate;

public record AcceptDonorAndCreatePendingDonationDto(
    String requestId,
    String personId,
    LocalDate expectedDate) {
}
