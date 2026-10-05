package bloodmatch.role.application;

import bloodmatch.auth.domain.SecurityRole;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.PartyRepositoryInterface;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.shared.application.exception.ConflictException;
import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.Email;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterBloodCenterUseCaseTest {

  private final BloodCenterRepositoryInterface bloodCenterRepository = mock(BloodCenterRepositoryInterface.class);
  private final PartyRepositoryInterface partyRepository = mock(PartyRepositoryInterface.class);
  private final UserAccountRepositoryInterface userAccountRepository = mock(UserAccountRepositoryInterface.class);

  private final RegisterBloodCenterUseCase useCase = new RegisterBloodCenterUseCase(
      bloodCenterRepository,
      partyRepository,
      userAccountRepository);

  @Test
  void shouldRegisterBloodCenterSuccessfully() {
    Organization org = organization();
    DomainID orgId = org.getId();
    String orgIdStr = orgId.getValue().toString();

    UserAccount userAccount = new UserAccount(
        orgId, new Email("center@bloodmatch.com"), "hash", Set.of());

    when(partyRepository.findById(orgId)).thenReturn(Optional.of(org));
    when(bloodCenterRepository.findByPartyId(orgId)).thenReturn(Optional.empty());
    when(userAccountRepository.findByPartyId(orgId)).thenReturn(Optional.of(userAccount));

    var output = useCase.execute(new RegisterBloodCenterUseCase.Input(orgIdStr));

    assertNotNull(output);
    assertNotNull(output.id());
    verify(bloodCenterRepository).save(any(BloodCenter.class));
    verify(userAccountRepository).save(userAccount);
    assertEquals(Set.of(SecurityRole.BLOOD_CENTER), userAccount.getRoles());
  }

  @Test
  void shouldRejectNullConstructorArgs() {
    assertThrows(IllegalArgumentException.class, () -> new RegisterBloodCenterUseCase(null, partyRepository, userAccountRepository));
    assertThrows(IllegalArgumentException.class, () -> new RegisterBloodCenterUseCase(bloodCenterRepository, null, userAccountRepository));
    assertThrows(IllegalArgumentException.class, () -> new RegisterBloodCenterUseCase(bloodCenterRepository, partyRepository, null));
  }

  @Test
  void shouldValidateInputAndNotFound() {
    assertThrows(ValidationException.class, () -> useCase.execute(null));
    assertThrows(ValidationException.class, () -> useCase.execute(new RegisterBloodCenterUseCase.Input("not-a-uuid")));

    DomainID orgId = DomainID.generate();
    when(partyRepository.findById(orgId)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class, () -> useCase.execute(new RegisterBloodCenterUseCase.Input(orgId.getValue().toString())));
  }

  @Test
  void shouldThrowConflictWhenAlreadyRegistered() {
    Organization org = organization();
    DomainID orgId = org.getId();

    when(partyRepository.findById(orgId)).thenReturn(Optional.of(org));
    when(bloodCenterRepository.findByPartyId(orgId)).thenReturn(Optional.of(new BloodCenter(org)));

    assertThrows(ConflictException.class, () -> useCase.execute(new RegisterBloodCenterUseCase.Input(orgId.getValue().toString())));
  }

  @Test
  void shouldThrowNotFoundWhenUserAccountMissing() {
    Organization org = organization();
    DomainID orgId = org.getId();

    when(partyRepository.findById(orgId)).thenReturn(Optional.of(org));
    when(bloodCenterRepository.findByPartyId(orgId)).thenReturn(Optional.empty());
    when(userAccountRepository.findByPartyId(orgId)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> useCase.execute(new RegisterBloodCenterUseCase.Input(orgId.getValue().toString())));
  }

  private Organization organization() {
    return new Organization("Hemocentro Regional", new PhoneNumber("1133334444"), new CNPJ("12345678000100"));
  }
}
