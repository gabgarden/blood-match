package bloodmatch.application.usecase.role;

import bloodmatch.domain.party.Organization;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.shared.valueObjects.DomainID;

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
  public BloodCenter execute(DomainID organizationId) {

    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");

    Organization organization = partyRepository.findById(organizationId)
        .filter(Organization.class::isInstance)
        .map(Organization.class::cast)
        .orElseThrow(() -> new IllegalArgumentException("Organization not found"));

    if (bloodCenterRepository.findByPartyId(organizationId).isPresent())
      throw new IllegalStateException("BloodCenter already registered for organization");

    BloodCenter bloodCenter = new BloodCenter(organization);
    bloodCenterRepository.save(bloodCenter);
    addRoleToUserAccount(organizationId, SecurityRole.BLOOD_CENTER);
    return bloodCenter;
  }

  private void addRoleToUserAccount(DomainID partyId, SecurityRole role) {
    UserAccount userAccount = userAccountRepository.findByPartyId(partyId)
        .orElseThrow(() -> new IllegalStateException("User account not found for party"));

    Set<SecurityRole> updatedRoles = new HashSet<>(userAccount.getRoles());
    updatedRoles.add(role);
    userAccount.updateRoles(updatedRoles);
    userAccountRepository.save(userAccount);
  }
}
