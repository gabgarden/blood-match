package bloodmatch.interfaces.rest.donation.createcompleted;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to record an already completed donation.")
public record CreateCompletedDonationDto(
    @Schema(description = "UUID of the donor.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String personId,
    @Schema(description = "UUID of the organization where the donation took place.", format = "uuid", example = "c56a4180-65aa-42ec-a945-5fd21dec0538", requiredMode = Schema.RequiredMode.REQUIRED) String organizationId,
    @Schema(description = "Date on which the donation took place.", format = "date", example = "2026-07-15", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate donationDate) {
}
