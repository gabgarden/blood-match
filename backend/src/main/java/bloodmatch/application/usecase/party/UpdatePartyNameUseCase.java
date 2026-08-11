package bloodmatch.application.usecase.party;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.party.Party;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdatePartyNameUseCase {

  private final PartyRepositoryInterface partyRepository;

  public UpdatePartyNameUseCase(PartyRepositoryInterface partyRepository) {
    if (partyRepository == null)
      throw new IllegalArgumentException("PartyRepository cannot be null");

    this.partyRepository = partyRepository;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");
    if (input.newName() == null || input.newName().isBlank())
      throw new ValidationException("newName cannot be blank");

    DomainID partyId = DomainIdParser.parse(input.partyId(), "partyId");

    Party party = partyRepository.findById(partyId)
        .orElseThrow(() -> new NotFoundException("Party not found"));

    party.changeName(input.newName());
    partyRepository.save(party);
    return Output.from(party);
  }

  public record Input(String partyId, String newName) {
  }

  public record Output(String id, String name) {
    public static Output from(Party party) {
      return new Output(party.getId().getValue().toString(), party.getName());
    }
  }
}
