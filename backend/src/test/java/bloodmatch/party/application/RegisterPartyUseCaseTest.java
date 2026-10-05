package bloodmatch.party.application;

import bloodmatch.party.application.RegisterPartyUseCase.OrganizationInput;
import bloodmatch.party.application.RegisterPartyUseCase.PersonInput;
import bloodmatch.party.domain.PartyRepositoryInterface;
import bloodmatch.party.domain.PersonRepositoryInterface;
import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.shared.infrastructure.external.notification.EmailConfirmationMailer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterPartyUseCaseTest {

  private final PartyRepositoryInterface partyRepository = mock(PartyRepositoryInterface.class);
  private final PersonRepositoryInterface personRepository = mock(PersonRepositoryInterface.class);
  private final UserAccountRepositoryInterface userAccountRepository = mock(UserAccountRepositoryInterface.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final EmailConfirmationMailer emailConfirmationMailer = mock(EmailConfirmationMailer.class);

  @Test
  void shouldKeepAccountEnabledWhenConfirmationIsNotRequired() {
    RegisterPartyUseCase useCase = newUseCase(false);
    when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(passwordEncoder.encode("password1")).thenReturn("hashed");

    useCase.registerPerson(personInput());

    ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
    verify(userAccountRepository).save(captor.capture());
    UserAccount saved = captor.getValue();
    assertTrue(saved.isEnabled());
    assertNull(saved.getConfirmationToken());
    verify(emailConfirmationMailer, never()).sendConfirmationEmail(any(), any(), any());
  }

  @Test
  void shouldDisableAccountAndSendEmailWhenConfirmationIsRequiredForPerson() {
    RegisterPartyUseCase useCase = newUseCase(true);
    when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(passwordEncoder.encode("password1")).thenReturn("hashed");

    useCase.registerPerson(personInput());

    ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
    verify(userAccountRepository, times(2)).save(captor.capture());
    UserAccount saved = captor.getValue();
    assertFalse(saved.isEnabled());
    assertNotNull(saved.getConfirmationToken());
    assertNotNull(saved.getConfirmationTokenExpiresAt());
    verify(emailConfirmationMailer).sendConfirmationEmail(
        eq("ana@bloodmatch.com"),
        eq("Ana Silva"),
        contains("/confirm-email?token="));
  }

  @Test
  void shouldDisableAccountAndSendEmailWhenConfirmationIsRequiredForOrganization() {
    RegisterPartyUseCase useCase = newUseCase(true);
    when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(passwordEncoder.encode("password1")).thenReturn("hashed");

    useCase.registerOrganization(organizationInput());

    ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);
    verify(userAccountRepository, times(2)).save(captor.capture());
    UserAccount saved = captor.getValue();
    assertFalse(saved.isEnabled());
    assertNotNull(saved.getConfirmationToken());
    verify(emailConfirmationMailer).sendConfirmationEmail(
        eq("org@bloodmatch.com"),
        eq("Hemocentro"),
        contains("/confirm-email?token="));
  }

  @Test
  void shouldRejectNullConstructorArgs() {
    assertThrows(IllegalArgumentException.class, () -> new RegisterPartyUseCase(null, personRepository, userAccountRepository, passwordEncoder, emailConfirmationMailer, false, "url"));
    assertThrows(IllegalArgumentException.class, () -> new RegisterPartyUseCase(partyRepository, null, userAccountRepository, passwordEncoder, emailConfirmationMailer, false, "url"));
    assertThrows(IllegalArgumentException.class, () -> new RegisterPartyUseCase(partyRepository, personRepository, null, passwordEncoder, emailConfirmationMailer, false, "url"));
    assertThrows(IllegalArgumentException.class, () -> new RegisterPartyUseCase(partyRepository, personRepository, userAccountRepository, null, emailConfirmationMailer, false, "url"));
    assertThrows(IllegalArgumentException.class, () -> new RegisterPartyUseCase(partyRepository, personRepository, userAccountRepository, passwordEncoder, null, false, "url"));
  }

  @Test
  void shouldRegisterPersonWithAddress() {
    RegisterPartyUseCase useCase = newUseCase(false);
    when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(any())).thenReturn("hashed");

    var input = new PersonInput(
        "Carlos", "11988887777", "12345678901", LocalDate.of(1990, 1, 1),
        "carlos@bloodmatch.com", "password123", "password123",
        "Rua das Flores, 123", "São Paulo", "SP", "01001-000");

    var output = useCase.registerPerson(input);
    assertNotNull(output);
    verify(personRepository).save(any());
  }

  @Test
  void shouldRegisterOrganizationWithAddress() {
    RegisterPartyUseCase useCase = newUseCase(false);
    when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
    when(passwordEncoder.encode(any())).thenReturn("hashed");

    var input = new OrganizationInput(
        "Banco de Sangue", "1133334444", "12345678000100",
        "banco@bloodmatch.com", "password123", "password123",
        "Av Paulista, 1000", "São Paulo", "SP", "01310-100");

    var output = useCase.registerOrganization(input);
    assertNotNull(output);
    verify(partyRepository).save(any());
  }

  @Test
  void shouldValidateInputsAndCredentials() {
    RegisterPartyUseCase useCase = newUseCase(false);

    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.registerPerson(null));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.registerOrganization(null));

    // blank email
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.registerPerson(
        new PersonInput("Nome", "11988887777", "12345678901", LocalDate.of(1990, 1, 1), "", "pass1234", "pass1234", null, null, null, null)));

    // blank password
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.registerPerson(
        new PersonInput("Nome", "11988887777", "12345678901", LocalDate.of(1990, 1, 1), "a@b.com", "", "", null, null, null, null)));

    // short password
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.registerPerson(
        new PersonInput("Nome", "11988887777", "12345678901", LocalDate.of(1990, 1, 1), "a@b.com", "1234", "1234", null, null, null, null)));

    // blank passwordConfirmation
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.registerPerson(
        new PersonInput("Nome", "11988887777", "12345678901", LocalDate.of(1990, 1, 1), "a@b.com", "pass1234", null, null, null, null, null)));

    // mismatch password
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.registerPerson(
        new PersonInput("Nome", "11988887777", "12345678901", LocalDate.of(1990, 1, 1), "a@b.com", "pass1234", "different", null, null, null, null)));
  }

  @Test
  void shouldThrowConflictWhenEmailAlreadyRegistered() {
    RegisterPartyUseCase useCase = newUseCase(false);
    when(userAccountRepository.findByEmail(any())).thenReturn(Optional.of(mock(UserAccount.class)));

    assertThrows(bloodmatch.shared.application.exception.ConflictException.class, () -> useCase.registerPerson(personInput()));
  }

  private RegisterPartyUseCase newUseCase(boolean requireEmailConfirmation) {
    return new RegisterPartyUseCase(
        partyRepository,
        personRepository,
        userAccountRepository,
        passwordEncoder,
        emailConfirmationMailer,
        requireEmailConfirmation,
        "http://localhost:5173");
  }

  private static PersonInput personInput() {
    return new PersonInput(
        "Ana Silva",
        "11988887777",
        "12345678901",
        LocalDate.of(1990, 1, 1),
        "ana@bloodmatch.com",
        "password1",
        "password1",
        null,
        null,
        null,
        null);
  }

  private static OrganizationInput organizationInput() {
    return new OrganizationInput(
        "Hemocentro",
        "1133334444",
        "12345678000100",
        "org@bloodmatch.com",
        "password1",
        "password1",
        null,
        null,
        null,
        null);
  }
}
