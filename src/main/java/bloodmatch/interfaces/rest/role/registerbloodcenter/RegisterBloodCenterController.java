package bloodmatch.interfaces.rest.role.registerbloodcenter;

import bloodmatch.application.usecase.role.RegisterBloodCenterUseCase;
import bloodmatch.application.usecase.role.RegisterBloodCenterUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

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

    var output = registerBloodCenterUseCase.execute(new Input(payload.organizationId()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RegisterBloodCenterResponseDto.from(output));
  }
}
