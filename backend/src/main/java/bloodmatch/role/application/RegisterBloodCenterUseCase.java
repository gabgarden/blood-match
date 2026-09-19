package bloodmatch.role.application;

import bloodmatch.shared.application.exception.ConflictException;
import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.application.shared.DomainIdParser;
import bloodmatch.party.domain.Organization;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.party.domain.PartyRepositoryInterface;
import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.auth.domain.SecurityRole;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.shared.domain.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
public class RegisterBloodCenterUseCase {

  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final PartyRepositoryInterface partyRepository;
  private final UserAccountRepositoryInterface userAccountRepository;

  public RegisterBloodCenterUseCase(
      BloodCenterRepositoryInterface bloodCenterRepository,
      PartyRepositoryInterface partyRepository,
      UserAccountRepositoryInterface userAccountRepository) {

    if (bloodCenterRepository == null)
      throw new IllegalArgumentException("BloodCenterRepository cannot be null");
    if (partyRepository == null)
      throw new IllegalArgumentException("PartyRepository cannot be null");
    if (userAccountRepository == null)
      throw new IllegalArgumentException("UserAccountRepository cannot be null");

    this.bloodCenterRepository = bloodCenterRepository;
    this.partyRepository = partyRepository;
    this.userAccountRepository = userAccountRepository;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");

    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");

    Organization organization = partyRepository.findById(organizationId)
        .filter(Organization.class::isInstance)
        .map(Organization.class::cast)
        .orElseThrow(() -> new NotFoundException("Organization not found"));

    if (bloodCenterRepository.findByPartyId(organizationId).isPresent())
      throw new ConflictException("BloodCenter already registered for organization");

    BloodCenter bloodCenter = new BloodCenter(organization);
    bloodCenterRepository.save(bloodCenter);
    addRoleToUserAccount(organizationId, SecurityRole.BLOOD_CENTER);
    return Output.from(bloodCenter);
  }

  private void addRoleToUserAccount(DomainID partyId, SecurityRole role) {
    UserAccount userAccount = userAccountRepository.findByPartyId(partyId)
        .orElseThrow(() -> new NotFoundException("User account not found for party"));

    Set<SecurityRole> updatedRoles = new HashSet<>(userAccount.getRoles());
    updatedRoles.add(role);
    userAccount.updateRoles(updatedRoles);
    userAccountRepository.save(userAccount);
  }

  public record Input(String organizationId) {
  }

  public record Output(String id) {
    public static Output from(BloodCenter bloodCenter) {
      return new Output(bloodCenter.getId().getValue().toString());
    }
  }
}
