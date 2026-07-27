package bloodmatch.interfaces.rest.role.registerbloodcenter;

import bloodmatch.application.usecase.role.RegisterBloodCenterUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
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
@Tag(name = "Register Blood Center", description = "Register a new blood center role to an Organization.")
@RequestMapping
public class RegisterBloodCenterController {

  private final RegisterBloodCenterUseCase registerBloodCenterUseCase;

  public RegisterBloodCenterController(RegisterBloodCenterUseCase registerBloodCenterUseCase) {
    this.registerBloodCenterUseCase = registerBloodCenterUseCase;
  }

  @PostMapping("/blood-centers")
  public ResponseEntity<Map<String, String>> registerBloodCenter(@RequestBody RegisterBloodCenterDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.organizationId()))
        throw new IllegalArgumentException("organizationId cannot be blank");

      DomainID id = parseDomainId(payload.organizationId(), "organizationId");
      BloodCenter bloodCenter = registerBloodCenterUseCase.execute(id);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of("id", bloodCenter.getId().getValue().toString()));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
