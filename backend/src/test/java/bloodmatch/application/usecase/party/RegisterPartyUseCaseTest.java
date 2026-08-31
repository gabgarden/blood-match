package bloodmatch.application.usecase.party;

import bloodmatch.application.usecase.party.RegisterPartyUseCase.OrganizationInput;
import bloodmatch.application.usecase.party.RegisterPartyUseCase.PersonInput;
import bloodmatch.domain.party.PartyRepositoryInterface;
import bloodmatch.domain.party.PersonRepositoryInterface;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.infra.external.notification.EmailConfirmationMailer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
