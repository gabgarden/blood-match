package bloodmatch.interfaces.rest.role.registerrequester;

import bloodmatch.application.usecase.role.RegisterRequesterUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requester registered successfully.")
public record RegisterRequesterResponseDto(
    @Schema(description = "Requester role UUID", format = "uuid") String id) {

  public static RegisterRequesterResponseDto from(Output output) {
    return new RegisterRequesterResponseDto(output.id());
  }
}
