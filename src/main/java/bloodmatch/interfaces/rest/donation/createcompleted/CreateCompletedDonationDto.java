package bloodmatch.interfaces.rest.donation.createcompleted;

import java.time.LocalDate;

public record CreateCompletedDonationDto(
    String personId,
    String organizationId,
    LocalDate donationDate) {
}