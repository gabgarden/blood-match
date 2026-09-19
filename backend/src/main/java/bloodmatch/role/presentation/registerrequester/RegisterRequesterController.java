package bloodmatch.role.presentation.registerrequester;

import bloodmatch.role.application.RegisterRequesterUseCase;
import bloodmatch.role.application.RegisterRequesterUseCase.Input;
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
    requireSamePartyOrAdmin(payload.partyId());

    var output = registerRequesterUseCase.execute(new Input(payload.partyId()));

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(RegisterRequesterResponseDto.from(output));
  }
}
