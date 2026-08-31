package bloodmatch.interfaces.rest.auth.confirmemail;

import bloodmatch.application.usecase.auth.ConfirmEmailUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of confirming a user e-mail address.")
public record ConfirmEmailResponseDto(
    @Schema(description = "Confirmation result message", example = "Email confirmed") String message,
    @Schema(description = "Confirmed e-mail address", format = "email", example = "user@bloodmatch.com") String email) {

  public static ConfirmEmailResponseDto from(Output output) {
    return new ConfirmEmailResponseDto(output.message(), output.email());
  }
}
