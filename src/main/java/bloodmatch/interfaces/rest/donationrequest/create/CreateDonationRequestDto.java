package bloodmatch.interfaces.rest.donationrequest.create;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to create a blood donation request.")
public record CreateDonationRequestDto(
    @Schema(description = "UUID of the requesting party.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String partyId,
    @Schema(description = "UUID of the organization where the request is being made.", format = "uuid", example = "c56a4180-65aa-42ec-a945-5fd21dec0538", requiredMode = Schema.RequiredMode.REQUIRED) String organizationId,
    @Schema(description = "Required blood type.", example = "O-", allowableValues = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}, requiredMode = Schema.RequiredMode.REQUIRED) String bloodTypeNeeded,
    @Schema(description = "Target number of blood bags.", example = "10", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED) Integer goalBloodBags,
    @Schema(description = "Deadline for fulfilling the request.", format = "date", example = "2026-08-31", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate dateLimit,
    @Schema(description = "Urgency level of the request.", example = "CRITICAL", allowableValues = {"LOW", "MEDIUM", "CRITICAL"}, requiredMode = Schema.RequiredMode.REQUIRED) String urgency) {
}
