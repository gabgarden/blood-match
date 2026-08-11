package bloodmatch.interfaces.rest.donation.reschedulependingdonation;

import bloodmatch.application.usecase.donation.reschedulependingdonation.ReschedulePendingDonationUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pending donation rescheduled successfully.")
public record ReschedulePendingDonationResponseDto(
    @Schema(description = "Donation UUID", format = "uuid") String id,
    @Schema(description = "Expected donation date", format = "date") String expectedDate,
    @Schema(description = "Donation status", example = "PENDING") String status) {

  public static ReschedulePendingDonationResponseDto from(
      ReschedulePendingDonationUseCase.Output output) {
    return new ReschedulePendingDonationResponseDto(
        output.id(),
        output.expectedDate().toString(),
        output.status());
  }
}
