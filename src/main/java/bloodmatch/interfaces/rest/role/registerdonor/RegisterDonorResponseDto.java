package bloodmatch.interfaces.rest.role.registerdonor;

import bloodmatch.application.usecase.role.RegisterDonorUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Donor registered successfully.")
public record RegisterDonorResponseDto(
    @Schema(description = "Donor role UUID", format = "uuid") String id) {

  public static RegisterDonorResponseDto from(Output output) {
    return new RegisterDonorResponseDto(output.id());
  }
}
