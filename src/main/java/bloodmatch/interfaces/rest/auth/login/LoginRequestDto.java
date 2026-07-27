package bloodmatch.interfaces.rest.auth.login;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credentials used to authenticate a user.")
public record LoginRequestDto(
    @Schema(description = "Registered user email address.", format = "email", example = "ana.silva@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email,
    @Schema(description = "Account password.", format = "password", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED) String password) {
}
