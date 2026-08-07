package bloodmatch.interfaces.rest.auth.login;

import bloodmatch.application.usecase.auth.AuthenticationService.Output;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Set;

@Schema(description = "Successful authentication response.")
public record LoginResponseDto(
    @Schema(description = "JWT access token") String accessToken,
    @Schema(description = "Token type", example = "Bearer") String tokenType,
    @Schema(description = "Token lifetime in milliseconds") long expiresIn,
    @Schema(description = "Security roles granted to the user") List<String> roles,
    @Schema(description = "Party UUID", format = "uuid") String partyId) {

  public static LoginResponseDto from(Output output) {
    Set<String> roles = output.roles();
    return new LoginResponseDto(
        output.accessToken(),
        output.tokenType(),
        output.expiresIn(),
        roles == null ? List.of() : roles.stream().sorted().toList(),
        output.partyId());
  }
}
