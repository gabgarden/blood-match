package bloodmatch.interfaces.rest.donationrequest.updatedonationrequestdatelimit;

import java.time.LocalDate;

public record UpdateDonationRequestDateLimitDto(
    String requestId,
    LocalDate newDateLimit)
 {
}
