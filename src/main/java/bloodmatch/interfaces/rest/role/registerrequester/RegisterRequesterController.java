package bloodmatch.interfaces.rest.role.registerrequester;

import bloodmatch.application.usecase.role.RegisterRequesterUseCase;
import bloodmatch.application.usecase.role.RegisterRequesterUseCase.Input;
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
@Tag(name = "Register Requester", description = "Register a new requester role to a Party.")
@RequestMapping
public class RegisterRequesterController {

  private final RegisterRequesterUseCase registerRequesterUseCase;

  public RegisterRequesterController(RegisterRequesterUseCase registerRequesterUseCase) {
    this.registerRequesterUseCase = registerRequesterUseCase;
  }

  @PostMapping("/requesters")
  public ResponseEntity<RegisterRequesterResponseDto> registerRequester(@RequestBody RegisterRequesterDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.partyId(), "partyId cannot be blank");

    var output = registerRequesterUseCase.execute(new Input(payload.partyId()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RegisterRequesterResponseDto.from(output));
  }
}
