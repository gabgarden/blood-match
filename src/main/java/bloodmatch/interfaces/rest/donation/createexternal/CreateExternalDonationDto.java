package bloodmatch.interfaces.rest.donation.createexternal;

import java.time.LocalDate;

public record CreateExternalDonationDto(
    String personId,
    String bloodCenterId,
    LocalDate donationDate) {
}
