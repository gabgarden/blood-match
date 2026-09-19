package bloodmatch.role.application;

import bloodmatch.shared.application.exception.ConflictException;
import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.application.shared.DomainIdParser;
import bloodmatch.party.domain.Party;
import bloodmatch.party.domain.PartyRepositoryInterface;
import bloodmatch.role.domain.requester.RequesterRepositoryInterface;
import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.auth.domain.SecurityRole;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.shared.domain.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class RegisterRequesterUseCase {

  private final RequesterRepositoryInterface requesterRepository;
  private final PartyRepositoryInterface partyRepository;
  private final UserAccountRepositoryInterface userAccountRepository;

  public RegisterRequesterUseCase(
      RequesterRepositoryInterface requesterRepository,
      PartyRepositoryInterface partyRepository,
      UserAccountRepositoryInterface userAccountRepository) {

    if (requesterRepository == null)
      throw new IllegalArgumentException("RequesterRepository cannot be null");
    if (partyRepository == null)
      throw new IllegalArgumentException("PartyRepository cannot be null");
    if (userAccountRepository == null)
      throw new IllegalArgumentException("UserAccountRepository cannot be null");

    this.requesterRepository = requesterRepository;
    this.partyRepository = partyRepository;
    this.userAccountRepository = userAccountRepository;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");

    DomainID partyId = DomainIdParser.parse(input.partyId(), "partyId");

    Party party = partyRepository.findById(partyId)
        .orElseThrow(() -> new NotFoundException("Party not found"));

    if (requesterRepository.findByPartyId(partyId).isPresent())
      throw new ConflictException("Requester already registered for party");

    Requester requester = new Requester(party);
    requesterRepository.save(requester);
    addRoleToUserAccount(partyId, SecurityRole.REQUESTER);
    return Output.from(requester);
  }

  private void addRoleToUserAccount(DomainID partyId, SecurityRole role) {
    UserAccount userAccount = userAccountRepository.findByPartyId(partyId)
        .orElseThrow(() -> new NotFoundException("User account not found for party"));

    Set<SecurityRole> updatedRoles = new HashSet<>(userAccount.getRoles());
    updatedRoles.add(role);
    userAccount.updateRoles(updatedRoles);
    userAccountRepository.save(userAccount);
  }

  public record Input(String partyId) {
  }

  public record Output(String id) {
    public static Output from(Requester requester) {
      return new Output(requester.getId().getValue().toString());
    }
  }
}
