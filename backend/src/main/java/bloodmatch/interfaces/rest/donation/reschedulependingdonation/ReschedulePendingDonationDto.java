package bloodmatch.interfaces.rest.donation.reschedulependingdonation;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to reschedule a pending donation.")
public record ReschedulePendingDonationDto(
    @Schema(description = "UUID of the pending donation.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String donationId,
    @Schema(description = "Version of the pending donation.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) Long version,
    @Schema(description = "New expected date for the donation.", format = "date", example = "2026-08-15", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate newExpectedDate) {
}