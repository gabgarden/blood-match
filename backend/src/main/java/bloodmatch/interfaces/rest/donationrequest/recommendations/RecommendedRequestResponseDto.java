package bloodmatch.interfaces.rest.donationrequest.recommendations;


import bloodmatch.application.usecase.donationrequest.recommendations.GetRecommendedRequestsUseCase.OutputItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Recommended donation request for a donor.")
public record RecommendedRequestResponseDto(
    @Schema(description = "Request UUID") String requestId,
    @Schema(description = "Blood center organization UUID") String organizationId,
    @Schema(description = "Blood type needed") String bloodTypeNeeded,
    @Schema(description = "Date limit", format = "date") LocalDate dateLimit,
    @Schema(description = "Blood center name") String bloodCenterName,
    @Schema(description = "Urgency level") String urgency,
    @Schema(description = "Distance in km") Double distanceInKm,
    @Schema(description = "Goal blood bags") int goalBloodBags,
    @Schema(description = "Fulfilled blood bags") int fulfilledBloodBags,
    @Schema(description = "Whether goal was reached") boolean goalReached,
    @Schema(description = "Latitude coordinate") Double latitude,
    @Schema(description = "Longitude coordinate") Double longitude) {

  public static RecommendedRequestResponseDto from(OutputItem item) {
    return new RecommendedRequestResponseDto(
        item.requestId(),
        item.organizationId(),
        item.bloodTypeNeeded(),
        item.dateLimit(),
        item.bloodCenterName(),
        item.urgency(),
        item.distanceInKm(),
        item.goalBloodBags(),
        item.fulfilledBloodBags(),
        item.goalReached(),
        item.latitude(),
        item.longitude());
  }
}
