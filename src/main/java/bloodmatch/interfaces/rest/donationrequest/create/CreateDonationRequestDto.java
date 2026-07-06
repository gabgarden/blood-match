package bloodmatch.interfaces.rest.donationrequest.create;

import java.time.LocalDate;

public record CreateDonationRequestDto(
    String partyId,
    String bloodCenterId,
    String bloodTypeNeeded,
    LocalDate dateLimit,
    String urgency) {
}
