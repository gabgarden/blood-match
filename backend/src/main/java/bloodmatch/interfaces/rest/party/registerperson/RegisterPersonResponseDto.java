package bloodmatch.interfaces.rest.party.registerperson;

import bloodmatch.application.usecase.party.RegisterPartyUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Person registered successfully.")
public record RegisterPersonResponseDto(
    @Schema(description = "Party UUID", format = "uuid") String id,
    @Schema(description = "Party type", example = "PERSON") String type) {

  public static RegisterPersonResponseDto from(Output output) {
    return new RegisterPersonResponseDto(output.id(), output.type());
  }
}
