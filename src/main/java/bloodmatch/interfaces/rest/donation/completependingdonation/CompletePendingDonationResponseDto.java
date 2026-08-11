package bloodmatch.interfaces.rest.donation.completependingdonation;

import bloodmatch.application.usecase.donation.completependingdonation.CompletePendingDonationUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pending donation completed successfully.")
public record CompletePendingDonationResponseDto(
    @Schema(description = "Donation UUID", format = "uuid") String id,
    @Schema(description = "Completion date", format = "date") String completionDate,
    @Schema(description = "Donation status", example = "COMPLETED") String status) {

  public static CompletePendingDonationResponseDto from(
      CompletePendingDonationUseCase.Output output) {
    return new CompletePendingDonationResponseDto(
        output.id(),
        output.completionDate().toString(),
        output.status());
  }
}
