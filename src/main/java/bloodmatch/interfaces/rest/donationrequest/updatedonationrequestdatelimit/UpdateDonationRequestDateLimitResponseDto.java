package bloodmatch.interfaces.rest.donationrequest.updatedonationrequestdatelimit;

import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestDateLimitUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Donation request date limit updated successfully.")
public record UpdateDonationRequestDateLimitResponseDto(
    @Schema(description = "Request UUID", format = "uuid") String id,
    @Schema(description = "Updated date limit", format = "date") String dateLimit) {

  public static UpdateDonationRequestDateLimitResponseDto from(Output output) {
    return new UpdateDonationRequestDateLimitResponseDto(
        output.id(),
        output.dateLimit().toString());
  }
}
