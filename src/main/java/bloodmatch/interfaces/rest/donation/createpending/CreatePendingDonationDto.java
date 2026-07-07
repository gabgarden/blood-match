package bloodmatch.interfaces.rest.donation.createpending;

import java.time.LocalDate;

public record CreatePendingDonationDto(
    String requestId,
    String personId,
    LocalDate expectedDate) {
}
