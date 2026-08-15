package bloodmatch.interfaces.rest.auth.resendconfirmation;

import bloodmatch.application.usecase.auth.ResendConfirmationUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of requesting a new confirmation e-mail.")
public record ResendConfirmationResponseDto(
    @Schema(description = "Generic result message that does not reveal whether the account exists.") String message) {

  public static ResendConfirmationResponseDto from(Output output) {
    return new ResendConfirmationResponseDto(output.message());
  }
}
