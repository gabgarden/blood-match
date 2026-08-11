package bloodmatch.interfaces.rest.party.registerorganization;

import bloodmatch.application.usecase.party.RegisterPartyUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Organization registered successfully.")
public record RegisterOrganizationResponseDto(
    @Schema(description = "Party UUID", format = "uuid") String id,
    @Schema(description = "Party type", example = "ORGANIZATION") String type) {

  public static RegisterOrganizationResponseDto from(Output output) {
    return new RegisterOrganizationResponseDto(output.id(), output.type());
  }
}
