package bloodmatch.request.presentation.updatedonationrequest;

import bloodmatch.request.application.UpdateDonationRequestUseCase;

import java.time.LocalDate;

public record UpdateDonationRequestResponseDto(
    String id,
    Long version,
    Integer goalBloodBags,
    LocalDate dateLimit) {

  public static UpdateDonationRequestResponseDto from(UpdateDonationRequestUseCase.Output output) {
    return new UpdateDonationRequestResponseDto(
        output.id(),
        output.version(),
        output.goalBloodBags(),
        output.dateLimit());
  }
}
