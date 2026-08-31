package bloodmatch.application.usecase.role;

import bloodmatch.application.exception.ConflictException;
import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.party.Party;
import bloodmatch.domain.party.PartyRepositoryInterface;
import bloodmatch.domain.roles.requester.RequesterRepositoryInterface;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.DomainID;
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
