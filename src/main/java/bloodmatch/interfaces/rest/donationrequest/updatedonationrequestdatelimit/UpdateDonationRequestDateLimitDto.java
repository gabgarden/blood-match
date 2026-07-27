package bloodmatch.interfaces.rest.donationrequest.updatedonationrequestdatelimit;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to update a donation request deadline.")
public record UpdateDonationRequestDateLimitDto(
    @Schema(description = "UUID of the donation request.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String requestId,
    @Schema(description = "New deadline for fulfilling the request.", format = "date", example = "2026-08-31", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate newDateLimit)
 {
}
