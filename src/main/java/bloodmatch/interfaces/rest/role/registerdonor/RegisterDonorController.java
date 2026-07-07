package bloodmatch.interfaces.rest.role.registerdonor;

import bloodmatch.application.usecase.role.RegisterDonorUseCase;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@RequestMapping
public class RegisterDonorController {

  private final RegisterDonorUseCase registerDonorUseCase;

  public RegisterDonorController(RegisterDonorUseCase registerDonorUseCase) {
    this.registerDonorUseCase = registerDonorUseCase;
  }

  @PostMapping("/donors")
  public ResponseEntity<Map<String, String>> registerDonor(@RequestBody RegisterDonorDto payload) {


  
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.personId()))
        throw new IllegalArgumentException("personId cannot be blank");
      if (isBlank(payload.bloodType()))
        throw new IllegalArgumentException("bloodType cannot be blank");
      if (payload.weight() == null)
        throw new IllegalArgumentException("weight cannot be null");

      DomainID id = parseDomainId(payload.personId(), "personId");
      Donor donor = registerDonorUseCase.execute(
          id,
          BloodType.of(payload.bloodType()),
          payload.weight());

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of("id", donor.getId().getValue().toString()));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
