package bloodmatch.interfaces.rest.auth.resendconfirmation;

import bloodmatch.application.usecase.auth.ResendConfirmationUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Resend Confirmation", description = "Resend the e-mail confirmation message.")
@RequestMapping("/auth")
public class ResendConfirmationController {

  private final ResendConfirmationUseCase resendConfirmationUseCase;

  public ResendConfirmationController(ResendConfirmationUseCase resendConfirmationUseCase) {
    this.resendConfirmationUseCase = resendConfirmationUseCase;
  }

  @PostMapping("/resend-confirmation")
  public ResponseEntity<ResendConfirmationResponseDto> resend(@RequestBody ResendConfirmationRequestDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.email(), "email cannot be blank");

    var output = resendConfirmationUseCase.execute(payload.email());
    return ResponseEntity.ok(ResendConfirmationResponseDto.from(output));
  }
}
