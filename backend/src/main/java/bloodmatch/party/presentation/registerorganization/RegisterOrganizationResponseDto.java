package bloodmatch.party.presentation.registerorganization;

import bloodmatch.party.application.RegisterPartyUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Organization registered successfully.")
public record RegisterOrganizationResponseDto(
    @Schema(description = "Party UUID", format = "uuid") String id,
    @Schema(description = "Party type", example = "ORGANIZATION") String type,
    @Schema(description = "Whether the user must confirm e-mail before signing in") boolean emailConfirmationRequired) {

  public static RegisterOrganizationResponseDto from(Output output) {
    return new RegisterOrganizationResponseDto(output.id(), output.type(), output.emailConfirmationRequired());
  }
}
