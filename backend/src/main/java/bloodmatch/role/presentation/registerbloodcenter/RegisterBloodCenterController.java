package bloodmatch.role.presentation.registerbloodcenter;

import bloodmatch.role.application.RegisterBloodCenterUseCase;
import bloodmatch.role.application.RegisterBloodCenterUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.shared.presentation.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.shared.presentation.RequestValidationSupport.requireNonNull;
import static bloodmatch.shared.presentation.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Register Blood Center", description = "Register a new blood center role to an Organization.")
@RequestMapping
public class RegisterBloodCenterController {

  private final RegisterBloodCenterUseCase registerBloodCenterUseCase;

  public RegisterBloodCenterController(RegisterBloodCenterUseCase registerBloodCenterUseCase) {
    this.registerBloodCenterUseCase = registerBloodCenterUseCase;
  }

  @PostMapping("/blood-centers")
  public ResponseEntity<RegisterBloodCenterResponseDto> registerBloodCenter(
      @RequestBody RegisterBloodCenterDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.organizationId(), "organizationId cannot be blank");
    requireSamePartyOrAdmin(payload.organizationId());

    var output = registerBloodCenterUseCase.execute(new Input(payload.organizationId()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RegisterBloodCenterResponseDto.from(output));
  }
}
