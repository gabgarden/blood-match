package bloodmatch.interfaces.rest.role.updatedonorprofile;

import bloodmatch.application.usecase.role.UpdateDonorProfileUseCase;
import bloodmatch.application.usecase.role.UpdateDonorProfileUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Update Donor Profile", description = "Update the profile of an existing donor.")
@RequestMapping
public class UpdateDonorProfileController {

  private final UpdateDonorProfileUseCase updateDonorProfileUseCase;

  public UpdateDonorProfileController(UpdateDonorProfileUseCase updateDonorProfileUseCase) {
    this.updateDonorProfileUseCase = updateDonorProfileUseCase;
  }

  @PatchMapping("/donors/profile")
  public ResponseEntity<UpdateDonorProfileResponseDto> updateDonorProfile(
      @RequestBody UpdateDonorProfileDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.personId(), "personId cannot be blank");
    requireNotBlank(payload.bloodType(), "bloodType cannot be blank");
    requireNonNull(payload.weight(), "weight cannot be null");
    requireSamePartyOrAdmin(payload.personId());

    var output = updateDonorProfileUseCase.execute(new Input(
        payload.personId(),
        payload.bloodType(),
        payload.weight()));

    return ResponseEntity.ok(UpdateDonorProfileResponseDto.from(output));
  }
}
