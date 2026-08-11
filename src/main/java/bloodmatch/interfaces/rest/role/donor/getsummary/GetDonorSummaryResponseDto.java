package bloodmatch.interfaces.rest.role.donor.getsummary;

import bloodmatch.application.usecase.donor.getsummary.GetDonorSummaryUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Donor summary information.")
public record GetDonorSummaryResponseDto(
    @Schema(description = "Person UUID", format = "uuid") String personId,
    @Schema(description = "Donor display name") String donorName,
    @Schema(description = "Phone number") String phoneNumber,
    @Schema(description = "Blood type") String bloodType,
    @Schema(description = "Full address") String address,
    @Schema(description = "Last donation date", format = "date") LocalDate lastDonationDate,
    @Schema(description = "Days remaining until eligible to donate again") int daysRemaining,
    @Schema(description = "Estimated lives impacted by past donations") long livesImpacted) {

  public static GetDonorSummaryResponseDto from(Output output) {
    return new GetDonorSummaryResponseDto(
        output.personId(),
        output.donorName(),
        output.phoneNumber(),
        output.bloodType(),
        output.address(),
        output.lastDonationDate(),
        output.daysRemaining(),
        output.livesImpacted());
  }
}
