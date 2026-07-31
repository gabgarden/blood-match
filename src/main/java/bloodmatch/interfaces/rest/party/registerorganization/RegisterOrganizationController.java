package bloodmatch.interfaces.rest.party.registerorganization;

import bloodmatch.application.usecase.party.RegisterPartyUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import bloodmatch.domain.party.Organization;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;

@RestController
@Tag(name = "Register Organization", description = "Register a new organization.")
@RequestMapping("/parties")
public class RegisterOrganizationController {

  private final RegisterPartyUseCase registerPartyUseCase;

  public RegisterOrganizationController(RegisterPartyUseCase registerPartyUseCase) {
    this.registerPartyUseCase = registerPartyUseCase;
  }

  @PostMapping("/organizations")
  public ResponseEntity<Map<String, String>> registerOrganization(@RequestBody RegisterOrganizationDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.name()))
        throw new IllegalArgumentException("name cannot be blank");
      if (isBlank(payload.phoneNumber()))
        throw new IllegalArgumentException("phoneNumber cannot be blank");
      if (isBlank(payload.cnpj()))
        throw new IllegalArgumentException("cnpj cannot be blank");
      if (isBlank(payload.email()))
        throw new IllegalArgumentException("email cannot be blank");
      if (isBlank(payload.password()))
        throw new IllegalArgumentException("password cannot be blank");
      if (isBlank(payload.passwordConfirmation()))
        throw new IllegalArgumentException("passwordConfirmation cannot be blank");

      if ((payload.street() != null || payload.city() != null || payload.state() != null || payload.zipCode() != null) &&
          (isBlank(payload.street()) || isBlank(payload.city()) || isBlank(payload.state()) || isBlank(payload.zipCode())))
        throw new IllegalArgumentException("All address fields must be provided together");

      Organization organization = registerPartyUseCase.registerOrganization(
          payload.name(),
          payload.phoneNumber(),
          payload.cnpj(),
          payload.email(),
          payload.password(),
          payload.passwordConfirmation(),
          payload.street(),
          payload.city(),
          payload.state(),
          payload.zipCode());

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of(
              "id", organization.getId().getValue().toString(),
              "type", "ORGANIZATION"));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
