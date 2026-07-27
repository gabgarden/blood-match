package bloodmatch.interfaces.rest.auth.login;

import bloodmatch.application.usecase.auth.AuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;

@RestController
@Tag(name = "Authentication", description = "Login operation.")
@RequestMapping("/auth")
public class AuthController {

  private final AuthenticationService authenticationService;

  public AuthController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequestDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.email()))
        throw new IllegalArgumentException("email cannot be blank");
      if (isBlank(payload.password()))
        throw new IllegalArgumentException("password cannot be blank");

      AuthenticationService.AuthenticationResult result = authenticationService.authenticate(
          payload.email(),
          payload.password());

      return ResponseEntity.ok(Map.of(
          "accessToken", result.accessToken(),
          "tokenType", result.tokenType(),
          "expiresIn", result.expiresIn(),
          "roles", result.roles().stream().map(Enum::name).toList(),
          "partyId", result.partyId()));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(Map.of("error", e.getMessage()));
    }
  }
}
