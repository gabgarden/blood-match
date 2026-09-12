package bloodmatch.interfaces.rest.party.updateparty;

import bloodmatch.application.usecase.party.UpdatePartyUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/parties")
public class UpdatePartyController {

  private final UpdatePartyUseCase updatePartyUseCase;

  public UpdatePartyController(UpdatePartyUseCase updatePartyUseCase) {
    this.updatePartyUseCase = updatePartyUseCase;
  }

  @PatchMapping
  public ResponseEntity<UpdatePartyResponseDto> updateParty(@RequestBody UpdatePartyDto dto) {
    
    bloodmatch.interfaces.rest.shared.AuthenticatedPartySupport.requireSamePartyOrAdmin(dto.partyId());
      
    UpdatePartyUseCase.AddressInput addressInput = null;
    if (dto.address() != null) {
      addressInput = new UpdatePartyUseCase.AddressInput(
          dto.address().street(),
          dto.address().city(),
          dto.address().state(),
          dto.address().zipCode());
    }

    UpdatePartyUseCase.Input input = new UpdatePartyUseCase.Input(
        dto.partyId(),
        dto.version(),
        dto.name(),
        dto.phoneNumber(),
        addressInput);

    UpdatePartyUseCase.Output output = updatePartyUseCase.execute(input);

    return ResponseEntity.ok(UpdatePartyResponseDto.from(output));
  }
}
