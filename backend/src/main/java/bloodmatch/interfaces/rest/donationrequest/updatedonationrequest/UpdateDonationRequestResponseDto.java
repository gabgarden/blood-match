package bloodmatch.interfaces.rest.donationrequest.updatedonationrequest;

import bloodmatch.application.usecase.donationrequest.UpdateDonationRequestUseCase;

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
