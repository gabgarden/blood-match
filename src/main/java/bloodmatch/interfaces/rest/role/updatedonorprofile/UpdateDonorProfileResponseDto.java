package bloodmatch.interfaces.rest.role.updatedonorprofile;

import bloodmatch.application.usecase.role.UpdateDonorProfileUseCase.Output;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Donor profile updated successfully.")
public record UpdateDonorProfileResponseDto(
    @Schema(description = "Donor role UUID", format = "uuid") String id) {

  public static UpdateDonorProfileResponseDto from(Output output) {
    return new UpdateDonorProfileResponseDto(output.id());
  }
}
