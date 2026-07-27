package bloodmatch.interfaces.rest.role.updatedonorprofile;

import bloodmatch.application.usecase.role.UpdateDonorProfileUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@Tag(name = "Update Donor Profile", description = "Update the profile of an existing donor.")
@RequestMapping
public class UpdateDonorProfileController {

  private final UpdateDonorProfileUseCase updateDonorProfileUseCase;

  public UpdateDonorProfileController(UpdateDonorProfileUseCase updateDonorProfileUseCase) {
    this.updateDonorProfileUseCase = updateDonorProfileUseCase;
  }

  @PatchMapping("/donors/profile")
  public ResponseEntity<Map<String, String>> updateDonorProfile(@RequestBody UpdateDonorProfileDto payload) {
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
      Donor donor = updateDonorProfileUseCase.execute(
          id,
          BloodType.of(payload.bloodType()),
          payload.weight());

      return ResponseEntity.ok(Map.of("id", donor.getId().getValue().toString()));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
