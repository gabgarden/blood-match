package bloodmatch.interfaces.rest.auth.login;

import bloodmatch.application.usecase.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Authentication", description = "Login operation.")
@RequestMapping("/auth")
public class AuthController {

  private final AuthenticationService authenticationService;

  public AuthController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.email(), "email cannot be blank");
    requireNotBlank(payload.password(), "password cannot be blank");

    var output = authenticationService.authenticate(payload.email(), payload.password());

    return ResponseEntity.ok(LoginResponseDto.from(output));
  }
}
