package bloodmatch.request.presentation.updatedonationrequest;

import java.time.LocalDate;

public record UpdateDonationRequestDto(
    String requestId,
    Long version,
    Integer goalBloodBags,
    LocalDate dateLimit) {
}
