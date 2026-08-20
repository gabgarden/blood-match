package bloodmatch.interfaces.rest.donation.create;

import bloodmatch.application.shared.TimeFormats;
import bloodmatch.application.usecase.donation.create.CreateDonationUseCase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Donation created successfully.")
public record CreateDonationResponseDto(
    @Schema(description = "Donation UUID", format = "uuid") String id,
    @Schema(description = "Intended donation date, when scheduled", format = "date") LocalDate intendedDate,
    @Schema(description = "Actual donation date, when completed", format = "date") LocalDate donationDate,
    @Schema(description = "Expected donation time in HH:mm, if scheduled against a slot") String expectedTime,
    @Schema(description = "Donation status", example = "PENDING") String status) {

  public static CreateDonationResponseDto from(CreateDonationUseCase.Output output) {
    return new CreateDonationResponseDto(
        output.id(),
        output.intendedDate(),
        output.donationDate(),
        TimeFormats.format(output.expectedTime()),
        output.status());
  }
}
