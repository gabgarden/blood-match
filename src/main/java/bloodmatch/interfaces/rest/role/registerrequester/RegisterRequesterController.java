package bloodmatch.interfaces.rest.role.registerrequester;

import bloodmatch.application.usecase.role.RegisterRequesterUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import bloodmatch.domain.roles.requester.Requester;
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
@Tag(name = "Register Requester", description = "Register a new requester role to a Party.")
@RequestMapping
public class RegisterRequesterController {

  private final RegisterRequesterUseCase registerRequesterUseCase;

  public RegisterRequesterController(RegisterRequesterUseCase registerRequesterUseCase) {
    this.registerRequesterUseCase = registerRequesterUseCase;
  }

  @PostMapping("/requesters")
  public ResponseEntity<Map<String, String>> registerRequester(@RequestBody RegisterRequesterDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.partyId()))
        throw new IllegalArgumentException("partyId cannot be blank");

      DomainID id = parseDomainId(payload.partyId(), "partyId");
      Requester requester = registerRequesterUseCase.execute(id);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(Map.of("id", requester.getId().getValue().toString()));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
