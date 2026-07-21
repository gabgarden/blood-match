package bloodmatch.interfaces.rest.donation.completependingdonation;

import java.time.LocalDate;

public record CompletePendingDonationDto(
    String donationId,
    LocalDate completionDate) {
}
