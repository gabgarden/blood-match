package bloodmatch.interfaces.rest.auth.resendconfirmation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request to resend an e-mail confirmation message.")
public record ResendConfirmationRequestDto(
    @Schema(description = "Registered user email address.", format = "email", example = "ana.silva@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email) {
}
