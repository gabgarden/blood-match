package bloodmatch.interfaces.rest.party.updatepartyname;

import bloodmatch.application.usecase.party.UpdatePartyNameUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Party name updated successfully.")
public record UpdatePartyNameResponseDto(
    @Schema(description = "Party UUID", format = "uuid") String id,
    @Schema(description = "Updated party name") String name) {

  public static UpdatePartyNameResponseDto from(Output output) {
    return new UpdatePartyNameResponseDto(output.id(), output.name());
  }
}
