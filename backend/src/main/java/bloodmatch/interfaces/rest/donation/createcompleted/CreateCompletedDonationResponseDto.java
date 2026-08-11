package bloodmatch.interfaces.rest.donation.createcompleted;

import bloodmatch.application.usecase.donation.createcompleted.CreateCompletedDonationUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Completed donation created successfully.")
public record CreateCompletedDonationResponseDto(
    @Schema(description = "Donation UUID", format = "uuid") String id,
    @Schema(description = "Donation date", format = "date") String donationDate,
    @Schema(description = "Donation status", example = "COMPLETED") String status) {

  public static CreateCompletedDonationResponseDto from(
      CreateCompletedDonationUseCase.Output output) {
    return new CreateCompletedDonationResponseDto(
        output.id(),
        output.donationDate().toString(),
        output.status());
  }
}
