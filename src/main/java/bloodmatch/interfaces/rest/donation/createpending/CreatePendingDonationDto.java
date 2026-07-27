package bloodmatch.interfaces.rest.donation.createpending;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to schedule a pending donation.")
public record CreatePendingDonationDto(
    @Schema(description = "UUID of the organization where the donation is scheduled.", format = "uuid", example = "c56a4180-65aa-42ec-a945-5fd21dec0538", requiredMode = Schema.RequiredMode.REQUIRED) String organizationId,
    @Schema(description = "UUID of the donor.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String personId,
    @Schema(description = "Expected donation date.", format = "date", example = "2026-08-01", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate expectedDate) {
}
