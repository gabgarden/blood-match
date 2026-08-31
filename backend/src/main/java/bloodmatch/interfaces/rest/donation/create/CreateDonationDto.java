package bloodmatch.interfaces.rest.donation.create;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to create a donation. Send intendedDate to schedule, or donationDate to record a completed donation.")
public record CreateDonationDto(
    @Schema(description = "UUID of the donor.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String personId,
    @Schema(description = "UUID of the organization where the donation is scheduled or took place.", format = "uuid", example = "c56a4180-65aa-42ec-a945-5fd21dec0538", requiredMode = Schema.RequiredMode.REQUIRED) String organizationId,
    @Schema(description = "Intended donation date. Creates a pending donation. Mutually exclusive with donationDate.", format = "date", example = "2026-08-01", requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate intendedDate,
    @Schema(description = "Actual donation date. Creates a completed donation. Mutually exclusive with intendedDate.", format = "date", example = "2026-07-15", requiredMode = Schema.RequiredMode.NOT_REQUIRED) LocalDate donationDate,
    @Schema(description = "Optional expected time in HH:mm. Required when the blood center has slots for that intended date.", example = "08:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED) String expectedTime) {
}
