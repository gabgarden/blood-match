package bloodmatch.interfaces.rest.party.registerorganization;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.usecase.party.RegisterPartyUseCase;
import bloodmatch.application.usecase.party.RegisterPartyUseCase.OrganizationInput;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Register Organization", description = "Register a new organization.")
@RequestMapping("/parties")
public class RegisterOrganizationController {

  private final RegisterPartyUseCase registerPartyUseCase;

  public RegisterOrganizationController(RegisterPartyUseCase registerPartyUseCase) {
    this.registerPartyUseCase = registerPartyUseCase;
  }

  @PostMapping("/organizations")
  public ResponseEntity<RegisterOrganizationResponseDto> registerOrganization(
      @RequestBody RegisterOrganizationDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.name(), "name cannot be blank");
    requireNotBlank(payload.phoneNumber(), "phoneNumber cannot be blank");
    requireNotBlank(payload.cnpj(), "cnpj cannot be blank");
    requireNotBlank(payload.email(), "email cannot be blank");
    requireNotBlank(payload.password(), "password cannot be blank");
    requireNotBlank(payload.passwordConfirmation(), "passwordConfirmation cannot be blank");

    if ((payload.street() != null || payload.city() != null || payload.state() != null || payload.zipCode() != null)
        && (isBlank(payload.street()) || isBlank(payload.city()) || isBlank(payload.state()) || isBlank(payload.zipCode()))) {
      throw new ValidationException("All address fields must be provided together");
    }

    var output = registerPartyUseCase.registerOrganization(new OrganizationInput(
        payload.name(),
        payload.phoneNumber(),
        payload.cnpj(),
        payload.email(),
        payload.password(),
        payload.passwordConfirmation(),
        payload.street(),
        payload.city(),
        payload.state(),
        payload.zipCode()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RegisterOrganizationResponseDto.from(output));
  }
}
