package bloodmatch.interfaces.rest.donationrequest.updatedonationrequestgoalbloodbags;

import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestGoalBloodBagsUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Donation request goal blood bags updated successfully.")
public record UpdateDonationRequestGoalBloodBagsResponseDto(
    @Schema(description = "Request UUID", format = "uuid") String id,
    @Schema(description = "Updated goal blood bags") String goalBloodBags) {

  public static UpdateDonationRequestGoalBloodBagsResponseDto from(Output output) {
    return new UpdateDonationRequestGoalBloodBagsResponseDto(
        output.id(),
        String.valueOf(output.goalBloodBags()));
  }
}
