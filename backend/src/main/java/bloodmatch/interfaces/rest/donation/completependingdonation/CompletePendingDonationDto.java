package bloodmatch.interfaces.rest.donation.completependingdonation;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to complete a pending donation.")
public record CompletePendingDonationDto(
    @Schema(description = "UUID of the pending donation.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String donationId,
    @Schema(description = "Version of the pending donation.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long version,
    @Schema(description = "Date on which the donation was completed.", format = "date", example = "2026-07-27", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate completionDate) {
}
