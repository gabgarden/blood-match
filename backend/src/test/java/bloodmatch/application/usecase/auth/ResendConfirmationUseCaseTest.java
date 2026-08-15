package bloodmatch.application.usecase.auth;

import bloodmatch.domain.party.PartyRepositoryInterface;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import bloodmatch.infra.external.notification.EmailConfirmationMailer;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResendConfirmationUseCaseTest {

  private static final String MESSAGE =
      "If the email is registered and pending confirmation, a new message was sent.";

  private final UserAccountRepositoryInterface userAccountRepository = mock(UserAccountRepositoryInterface.class);
  private final PartyRepositoryInterface partyRepository = mock(PartyRepositoryInterface.class);
  private final EmailConfirmationMailer emailConfirmationMailer = mock(EmailConfirmationMailer.class);
  private final ResendConfirmationUseCase useCase = new ResendConfirmationUseCase(
      userAccountRepository,
      partyRepository,
      emailConfirmationMailer,
      "http://localhost:5173");

  @Test
  void shouldIssueNewTokenAndSendEmailWhenAccountIsPending() {
    UserAccount userAccount = pendingAccount("old-token");

    when(userAccountRepository.findByEmail(new Email("pending@bloodmatch.com")))
        .thenReturn(Optional.of(userAccount));
    when(partyRepository.findById(userAccount.getPartyId())).thenReturn(Optional.empty());

    ResendConfirmationUseCase.Output output = useCase.execute("pending@bloodmatch.com");

    assertEquals(MESSAGE, output.message());
    assertNotNull(userAccount.getConfirmationToken());
    assertNotEquals("old-token", userAccount.getConfirmationToken());
    verify(userAccountRepository).save(userAccount);
    verify(emailConfirmationMailer).sendConfirmationEmail(
        eq("pending@bloodmatch.com"),
        any(),
        contains("/confirm-email?token="));
  }

  @Test
  void shouldNotSendEmailWhenAccountIsAlreadyEnabled() {
    UserAccount userAccount = new UserAccount(
        DomainID.generate(),
        new Email("active@bloodmatch.com"),
        "hash",
        Set.of());

    when(userAccountRepository.findByEmail(new Email("active@bloodmatch.com")))
        .thenReturn(Optional.of(userAccount));

    ResendConfirmationUseCase.Output output = useCase.execute("active@bloodmatch.com");

    assertEquals(MESSAGE, output.message());
    verify(userAccountRepository, never()).save(any());
    verify(emailConfirmationMailer, never()).sendConfirmationEmail(any(), any(), any());
  }

  @Test
  void shouldNotRevealMissingAccount() {
    when(userAccountRepository.findByEmail(new Email("missing@bloodmatch.com")))
        .thenReturn(Optional.empty());

    ResendConfirmationUseCase.Output output = useCase.execute("missing@bloodmatch.com");

    assertEquals(MESSAGE, output.message());
    verify(emailConfirmationMailer, never()).sendConfirmationEmail(any(), any(), any());
  }

  @Test
  void shouldNotRevealInvalidEmailFormat() {
    ResendConfirmationUseCase.Output output = useCase.execute("not-an-email");

    assertEquals(MESSAGE, output.message());
    verify(emailConfirmationMailer, never()).sendConfirmationEmail(any(), any(), any());
  }

  private static UserAccount pendingAccount(String token) {
    UserAccount userAccount = new UserAccount(
        DomainID.generate(),
        new Email("pending@bloodmatch.com"),
        "hash",
        Set.of());
    userAccount.startEmailConfirmation(token, LocalDateTime.now().plusHours(1));
    return userAccount;
  }
}
