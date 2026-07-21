package bloodmatch.interfaces.rest.donationrequest.create;

import java.time.LocalDate;

public record CreateDonationRequestDto(
    String partyId,
    String organizationId,
    String bloodTypeNeeded,
    Integer goalBloodBags,
    LocalDate dateLimit,
    String urgency) {
}
