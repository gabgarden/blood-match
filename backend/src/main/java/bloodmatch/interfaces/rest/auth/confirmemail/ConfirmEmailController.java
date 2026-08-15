package bloodmatch.interfaces.rest.auth.confirmemail;

import bloodmatch.application.usecase.auth.ConfirmEmailUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Confirm Email", description = "Confirm a user e-mail address using a token.")
@RequestMapping("/auth")
public class ConfirmEmailController {

  private final ConfirmEmailUseCase confirmEmailUseCase;

  public ConfirmEmailController(ConfirmEmailUseCase confirmEmailUseCase) {
    this.confirmEmailUseCase = confirmEmailUseCase;
  }

  @GetMapping("/confirm-email")
  public ResponseEntity<ConfirmEmailResponseDto> confirmGet(@RequestParam(required = false) String token) {
    requireNotBlank(token, "token cannot be blank");
    return ResponseEntity.ok(ConfirmEmailResponseDto.from(confirmEmailUseCase.execute(token)));
  }

  @PostMapping("/confirm-email")
  public ResponseEntity<ConfirmEmailResponseDto> confirmPost(@RequestBody ConfirmEmailRequestDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.token(), "token cannot be blank");
    return ResponseEntity.ok(ConfirmEmailResponseDto.from(confirmEmailUseCase.execute(payload.token())));
  }
}
