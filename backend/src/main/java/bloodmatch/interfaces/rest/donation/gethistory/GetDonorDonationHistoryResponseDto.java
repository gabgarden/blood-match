package bloodmatch.interfaces.rest.donation.gethistory;

import bloodmatch.application.usecase.donation.gethistory.GetDonorDonationHistoryUseCase.OutputItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "A donation in the donor's history.")
public record GetDonorDonationHistoryResponseDto(
    @Schema(description = "Donation UUID", format = "uuid") String donationId,
    @Schema(description = "Donation date", format = "date") LocalDate date,
    @Schema(description = "Blood center name") String location) {

  public static GetDonorDonationHistoryResponseDto from(OutputItem item) {
    return new GetDonorDonationHistoryResponseDto(
        item.donationId(),
        item.date(),
        item.location());
  }
}
