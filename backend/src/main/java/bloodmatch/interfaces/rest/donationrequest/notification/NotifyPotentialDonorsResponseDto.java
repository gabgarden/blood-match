package bloodmatch.interfaces.rest.donationrequest.notification;

import bloodmatch.application.usecase.donationrequest.notification.NotifyPotentialDonorsUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of notifying potential donors.")
public record NotifyPotentialDonorsResponseDto(
    @Schema(description = "Status message") String message) {

  public static NotifyPotentialDonorsResponseDto from(Output output) {
    return new NotifyPotentialDonorsResponseDto(output.message());
  }
}
