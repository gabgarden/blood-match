package bloodmatch.role.presentation.updatedonor;

import bloodmatch.role.application.UpdateDonorUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/donors")
public class UpdateDonorController {

  private final UpdateDonorUseCase updateDonorUseCase;

  public UpdateDonorController(UpdateDonorUseCase updateDonorUseCase) {
    this.updateDonorUseCase = updateDonorUseCase;
  }

  @PatchMapping
  public ResponseEntity<UpdateDonorResponseDto> updateDonor(@RequestBody UpdateDonorDto dto) {
      
    bloodmatch.shared.presentation.AuthenticatedPartySupport.requireSamePartyOrAdmin(dto.personId());

    UpdateDonorUseCase.Input input = new UpdateDonorUseCase.Input(
        dto.personId(),
        dto.version(),
        dto.bloodType(),
        dto.weight(),
        dto.maxDistanceInKm());

    UpdateDonorUseCase.Output output = updateDonorUseCase.execute(input);

    return ResponseEntity.ok(UpdateDonorResponseDto.from(output));
  }
}
