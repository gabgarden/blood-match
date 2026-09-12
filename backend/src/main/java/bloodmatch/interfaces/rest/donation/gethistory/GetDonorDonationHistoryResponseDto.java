package bloodmatch.interfaces.rest.donation.gethistory;

import bloodmatch.application.usecase.donation.gethistory.GetDonorDonationHistoryUseCase.OutputItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "A donation in the donor's history.")
public record GetDonorDonationHistoryResponseDto(
    @Schema(description = "Donation UUID", format = "uuid") String donationId,
    @Schema(description = "Donation version", example = "1") Long version,
    @Schema(description = "Donation date", format = "date") LocalDate date,
    @Schema(description = "Blood center name") String location,
    @Schema(description = "PENDING, COMPLETED or CANCELLED", example = "COMPLETED") String status) {

  public static GetDonorDonationHistoryResponseDto from(OutputItem item) {
    return new GetDonorDonationHistoryResponseDto(
        item.donationId(),
        item.version(),
        item.date(),
        item.location(),
        item.status());
  }
}
