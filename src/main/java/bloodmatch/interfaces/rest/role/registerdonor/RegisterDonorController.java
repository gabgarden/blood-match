package bloodmatch.interfaces.rest.role.registerdonor;

import bloodmatch.application.usecase.role.RegisterDonorUseCase;
import bloodmatch.application.usecase.role.RegisterDonorUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Register Donor", description = "Register a new donor role to a Person.")
@RequestMapping
public class RegisterDonorController {

  private final RegisterDonorUseCase registerDonorUseCase;

  public RegisterDonorController(RegisterDonorUseCase registerDonorUseCase) {
    this.registerDonorUseCase = registerDonorUseCase;
  }

  @PostMapping("/donors")
  public ResponseEntity<RegisterDonorResponseDto> registerDonor(@RequestBody RegisterDonorDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.personId(), "personId cannot be blank");
    requireNotBlank(payload.bloodType(), "bloodType cannot be blank");
    requireNonNull(payload.weight(), "weight cannot be null");
    requireSamePartyOrAdmin(payload.personId());

    var output = registerDonorUseCase.execute(new Input(
        payload.personId(),
        payload.bloodType(),
        payload.weight()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RegisterDonorResponseDto.from(output));
  }
}
