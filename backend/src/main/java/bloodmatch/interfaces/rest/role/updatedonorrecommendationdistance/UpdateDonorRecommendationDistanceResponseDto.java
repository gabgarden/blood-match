package bloodmatch.interfaces.rest.role.updatedonorrecommendationdistance;

import bloodmatch.application.usecase.role.UpdateDonorRecommendationDistanceUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Donor recommendation distance updated successfully.")
public record UpdateDonorRecommendationDistanceResponseDto(
    @Schema(description = "Person UUID", format = "uuid") String personId,
    @Schema(description = "Maximum recommendation distance in kilometers") double maxDistanceInKm) {

  public static UpdateDonorRecommendationDistanceResponseDto from(Output output) {
    return new UpdateDonorRecommendationDistanceResponseDto(output.personId(), output.maxDistanceInKm());
  }
}
