package bloodmatch.interfaces.rest.donationrequest.updatedonationrequestgoalbloodbags;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to update a donation request blood bag target.")
public record UpdateDonationRequestGoalBloodBagsDto(
    @Schema(description = "UUID of the donation request.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String requestId,
    @Schema(description = "New target number of blood bags.", example = "15", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED) int newGoalBloodBags)
 {
}
