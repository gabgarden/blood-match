package bloodmatch.interfaces.rest.auth.confirmemail;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token used to confirm a user e-mail address.")
public record ConfirmEmailRequestDto(
    @Schema(description = "E-mail confirmation token.", requiredMode = Schema.RequiredMode.REQUIRED) String token) {
}
