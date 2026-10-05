package bloodmatch.role.application;

import bloodmatch.role.application.RegisterDonorUseCase.Input;
import bloodmatch.role.application.RegisterDonorUseCase.Output;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.party.domain.PersonRepositoryInterface;
import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.auth.domain.SecurityRole;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.shared.domain.services.GeocodingServiceInterface;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.Email;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        new PhoneNumber("11988887777"),
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

    Output output = useCase.execute(new Input(partyId.getValue().toString(), "A+", 72.5, null));

    assertNotNull(output.id());
    assertTrue(userAccount.getRoles().contains(SecurityRole.DONOR));
    verify(donorRepository).save(any());
    verify(userAccountRepository).save(userAccount);
  }

  @Test
  void shouldSetLastDonationDateWhenProvided() {
    Person person = newPerson();
    stubSuccessfulLookup(person);
    LocalDate lastDonationDate = LocalDate.now().minusDays(30);

    useCase.execute(new Input(person.getId().getValue().toString(), "A+", 72.5, lastDonationDate));

    ArgumentCaptor<Donor> captor = ArgumentCaptor.forClass(Donor.class);
    verify(donorRepository).save(captor.capture());
    assertEquals(lastDonationDate, captor.getValue().getLastDonationDate());
  }

  @Test
  void shouldLeaveLastDonationDateNullWhenNotProvided() {
    Person person = newPerson();
    stubSuccessfulLookup(person);

    useCase.execute(new Input(person.getId().getValue().toString(), "A+", 72.5, null));

    ArgumentCaptor<Donor> captor = ArgumentCaptor.forClass(Donor.class);
    verify(donorRepository).save(captor.capture());
    assertNull(captor.getValue().getLastDonationDate());
  }

  @Test
  void shouldRejectNullConstructorArgs() {
    assertThrows(IllegalArgumentException.class, () -> new RegisterDonorUseCase(null, personRepository, userAccountRepository, geocodingService));
    assertThrows(IllegalArgumentException.class, () -> new RegisterDonorUseCase(donorRepository, null, userAccountRepository, geocodingService));
    assertThrows(IllegalArgumentException.class, () -> new RegisterDonorUseCase(donorRepository, personRepository, null, geocodingService));
    assertThrows(IllegalArgumentException.class, () -> new RegisterDonorUseCase(donorRepository, personRepository, userAccountRepository, null));
  }

  @Test
  void shouldGeocodePersonAddressWhenNoCoordinates() {
    Person person = newPerson();
    person.changeAddress(new bloodmatch.shared.domain.valueObjects.Address("Rua A", "SP", "SP", "01001-000"));
    stubSuccessfulLookup(person);

    when(geocodingService.getCoordinatesFromAddress(any())).thenReturn(
        new bloodmatch.shared.domain.valueObjects.Address("Rua A", "SP", "SP", "01001-000", -23.5, -46.6));

    useCase.execute(new Input(person.getId().getValue().toString(), "A+", 70.0, null));
    verify(geocodingService).getCoordinatesFromAddress(any());
    verify(personRepository).save(person);
  }

  @Test
  void shouldValidateInputAndExceptions() {
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(null));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input("id", null, 70.0, null)));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input("id", "", 70.0, null)));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(DomainID.generate().getValue().toString(), "INVALID", 70.0, null)));
  }

  @Test
  void shouldThrowWhenPersonNotFoundOrAlreadyRegisteredOrUserAccountMissing() {
    DomainID personId = DomainID.generate();
    when(personRepository.findById(personId)).thenReturn(Optional.empty());
    assertThrows(bloodmatch.shared.application.exception.NotFoundException.class, () -> useCase.execute(
        new Input(personId.getValue().toString(), "A+", 70.0, null)));

    Person person = newPerson();
    when(personRepository.findById(person.getId())).thenReturn(Optional.of(person));
    when(donorRepository.findByPartyId(person.getId())).thenReturn(Optional.of(mock(Donor.class)));
    assertThrows(bloodmatch.shared.application.exception.ConflictException.class, () -> useCase.execute(
        new Input(person.getId().getValue().toString(), "A+", 70.0, null)));

    when(donorRepository.findByPartyId(person.getId())).thenReturn(Optional.empty());
    when(userAccountRepository.findByPartyId(person.getId())).thenReturn(Optional.empty());
    assertThrows(bloodmatch.shared.application.exception.NotFoundException.class, () -> useCase.execute(
        new Input(person.getId().getValue().toString(), "A+", 70.0, null)));
  }

  @Test
  void shouldThrowValidationExceptionWhenDonorDomainThrows() {
    Person person = newPerson();
    stubSuccessfulLookup(person);

    // Negative weight causes domain exception
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(person.getId().getValue().toString(), "A+", -10.0, null)));
  }

  private Person newPerson() {
    return new Person(
        "Donor Person",
        new PhoneNumber("11988887777"),
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1));
  }

  private void stubSuccessfulLookup(Person person) {
    DomainID partyId = person.getId();
    UserAccount userAccount = new UserAccount(
        partyId,
        new Email("donor@bloodmatch.com"),
        "hash",
        Set.of());
    when(personRepository.findById(partyId)).thenReturn(Optional.of(person));
    when(donorRepository.findByPartyId(partyId)).thenReturn(Optional.empty());
    when(userAccountRepository.findByPartyId(partyId)).thenReturn(Optional.of(userAccount));
  }
}
