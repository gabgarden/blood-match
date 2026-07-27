package bloodmatch.interfaces.rest.role.updatedonorrecommendationdistance;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to configure the maximum distance for donor recommendations.")
public record UpdateDonorRecommendationDistanceDto(
    @Schema(description = "UUID of the donor.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String personId,
    @Schema(description = "Maximum distance, in kilometers, for recommended donation requests.", example = "30", minimum = "0.1", defaultValue = "30", requiredMode = Schema.RequiredMode.REQUIRED) Double maxDistanceInKm) {
}
