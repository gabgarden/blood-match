package bloodmatch.application.usecase.auth;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfirmEmailUseCaseTest {

  private final UserAccountRepositoryInterface userAccountRepository = mock(UserAccountRepositoryInterface.class);
  private final ConfirmEmailUseCase useCase = new ConfirmEmailUseCase(userAccountRepository);

  @Test
  void shouldConfirmEmailWhenTokenIsValid() {
    UserAccount userAccount = pendingAccount("valid-token", LocalDateTime.now().plusHours(24));
    when(userAccountRepository.findByEmailConfirmationToken("valid-token"))
        .thenReturn(Optional.of(userAccount));

    ConfirmEmailUseCase.Output output = useCase.execute("valid-token");

    assertEquals("Email confirmed", output.message());
    assertEquals("pending@bloodmatch.com", output.email());
    assertTrue(userAccount.isEnabled());
    assertNull(userAccount.getConfirmationToken());
    assertNull(userAccount.getConfirmationTokenExpiresAt());
    verify(userAccountRepository).save(userAccount);
  }

  @Test
  void shouldRejectBlankToken() {
    ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute(" "));
    assertEquals("Invalid confirmation token", ex.getMessage());
  }

  @Test
  void shouldRejectUnknownToken() {
    when(userAccountRepository.findByEmailConfirmationToken("missing"))
        .thenReturn(Optional.empty());

    ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute("missing"));
    assertEquals("Invalid confirmation token", ex.getMessage());
  }

  @Test
  void shouldRejectExpiredToken() {
    UserAccount userAccount = pendingAccount("expired-token", LocalDateTime.now().minusMinutes(1));
    when(userAccountRepository.findByEmailConfirmationToken("expired-token"))
        .thenReturn(Optional.of(userAccount));

    ValidationException ex = assertThrows(ValidationException.class, () -> useCase.execute("expired-token"));
    assertEquals("Confirmation token expired", ex.getMessage());
  }

  private static UserAccount pendingAccount(String token, LocalDateTime expiresAt) {
    UserAccount userAccount = new UserAccount(
        DomainID.generate(),
        new Email("pending@bloodmatch.com"),
        "hash",
        Set.of(SecurityRole.DONOR));
    userAccount.startEmailConfirmation(token, expiresAt);
    return userAccount;
  }
}
