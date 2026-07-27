package bloodmatch.interfaces.rest.party.updatepartyname;

import bloodmatch.application.usecase.party.UpdatePartyNameUseCase;
import io.swagger.v3.oas.annotations.tags.Tag;
import bloodmatch.domain.party.Party;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.isBlank;
import static bloodmatch.interfaces.rest.shared.RequestValidationSupport.parseDomainId;

@RestController
@Tag(name = "Update Party Name", description = "Update the name of an existing party.")
@RequestMapping("/parties")
public class UpdatePartyNameController {

  private final UpdatePartyNameUseCase updatePartyNameUseCase;

  public UpdatePartyNameController(UpdatePartyNameUseCase updatePartyNameUseCase) {
    this.updatePartyNameUseCase = updatePartyNameUseCase;
  }

  @PatchMapping("/name")
  public ResponseEntity<Map<String, String>> updatePartyName(@RequestBody UpdatePartyNameDto payload) {
    try {
      if (payload == null)
        throw new IllegalArgumentException("Request body cannot be null");
      if (isBlank(payload.partyId()))
        throw new IllegalArgumentException("partyId cannot be blank");
      if (isBlank(payload.newName()))
        throw new IllegalArgumentException("newName cannot be blank");

      DomainID id = parseDomainId(payload.partyId(), "partyId");
      Party party = updatePartyNameUseCase.execute(id, payload.newName());

      return ResponseEntity.ok(Map.of(
          "id", party.getId().getValue().toString(),
          "name", party.getName()));

    } catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}
