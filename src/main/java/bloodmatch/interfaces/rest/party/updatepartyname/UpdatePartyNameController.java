package bloodmatch.interfaces.rest.party.updatepartyname;

import bloodmatch.application.usecase.party.UpdatePartyNameUseCase;
import bloodmatch.application.usecase.party.UpdatePartyNameUseCase.Input;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNonNull;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.requireNotBlank;

@RestController
@Tag(name = "Update Party Name", description = "Update the name of an existing party.")
@RequestMapping("/parties")
public class UpdatePartyNameController {

  private final UpdatePartyNameUseCase updatePartyNameUseCase;

  public UpdatePartyNameController(UpdatePartyNameUseCase updatePartyNameUseCase) {
    this.updatePartyNameUseCase = updatePartyNameUseCase;
  }

  @PatchMapping("/name")
  public ResponseEntity<UpdatePartyNameResponseDto> updatePartyName(@RequestBody UpdatePartyNameDto payload) {
    requireNonNull(payload, "Request body cannot be null");
    requireNotBlank(payload.partyId(), "partyId cannot be blank");
    requireNotBlank(payload.newName(), "newName cannot be blank");
    requireSamePartyOrAdmin(payload.partyId());

    var output = updatePartyNameUseCase.execute(new Input(payload.partyId(), payload.newName()));

    return ResponseEntity.ok(UpdatePartyNameResponseDto.from(output));
  }
}
