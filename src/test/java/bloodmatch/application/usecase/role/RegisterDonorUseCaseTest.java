package bloodmatch.application.usecase.role;

import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.repositories.PersonRepositoryInterface;
import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import org.junit.jupiter.api.Test;
import bloodmatch.domain.services.GeocodingServiceInterface;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterDonorUseCaseTest {

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final PersonRepositoryInterface personRepository = mock(PersonRepositoryInterface.class);
  private final UserAccountRepositoryInterface userAccountRepository = mock(UserAccountRepositoryInterface.class);
  private final GeocodingServiceInterface geocodingService = mock(GeocodingServiceInterface.class);

  private final RegisterDonorUseCase useCase = new RegisterDonorUseCase(
      donorRepository,
      personRepository,
      userAccountRepository,
      geocodingService);

  @Test
  void shouldAddDonorRoleToUserAccountWhenRegisteringDonor() {
    Person person = new Person(
        "Donor Person",
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1));
    DomainID partyId = person.getId();

    UserAccount userAccount = new UserAccount(
        partyId,
        new Email("donor@bloodmatch.com"),
        "hash",
        Set.of());

    when(personRepository.findById(partyId)).thenReturn(Optional.of(person));
    when(donorRepository.findByPartyId(partyId)).thenReturn(Optional.empty());
    when(userAccountRepository.findByPartyId(partyId)).thenReturn(Optional.of(userAccount));

    useCase.execute(partyId, BloodType.of("A+"), 72.5);

    assertTrue(userAccount.getRoles().contains(SecurityRole.DONOR));
    verify(donorRepository).save(any());
    verify(userAccountRepository).save(userAccount);
  }
}
