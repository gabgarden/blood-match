package bloodmatch.interfaces.rest.donation.createpending;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Pending donation created successfully.")
public record CreatePendingDonationResponseDto(
    @Schema(description = "Donation UUID", format = "uuid") String id,
    @Schema(description = "Expected donation date", format = "date") String expectedDate,
    @Schema(description = "Donation status", example = "PENDING") String status) {

  public static CreatePendingDonationResponseDto from(
      bloodmatch.application.usecase.donation.createpending.CreatePendingDonationUseCase.Output output) {
    return new CreatePendingDonationResponseDto(
        output.id(),
        output.expectedDate().toString(),
        output.status());
  }
}
