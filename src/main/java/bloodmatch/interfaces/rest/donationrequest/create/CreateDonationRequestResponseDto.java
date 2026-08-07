package bloodmatch.interfaces.rest.donationrequest.create;

import bloodmatch.application.usecase.donationrequest.CreateDonationRequestUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Donation request created successfully.")
public record CreateDonationRequestResponseDto(
    @Schema(description = "Request UUID", format = "uuid") String id) {

  public static CreateDonationRequestResponseDto from(Output output) {
    return new CreateDonationRequestResponseDto(output.id());
  }
}
