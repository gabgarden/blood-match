package bloodmatch.interfaces.rest.role.registerbloodcenter;

import bloodmatch.application.usecase.role.RegisterBloodCenterUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Blood center registered successfully.")
public record RegisterBloodCenterResponseDto(
    @Schema(description = "Blood center role UUID", format = "uuid") String id) {

  public static RegisterBloodCenterResponseDto from(Output output) {
    return new RegisterBloodCenterResponseDto(output.id());
  }
}
