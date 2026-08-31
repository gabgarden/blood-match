package bloodmatch.domain.security;

import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAccountTest {

  @Test
  void shouldCreateEnabledAccountWithoutConfirmationToken() {
    UserAccount account = newAccount();

    assertTrue(account.isEnabled());
    assertNull(account.getConfirmationToken());
    assertNull(account.getConfirmationTokenExpiresAt());
  }

  @Test
  void startEmailConfirmationShouldDisableAccountAndStoreToken() {
    UserAccount account = newAccount();
    LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);

    account.startEmailConfirmation("token-abc", expiresAt);

    assertFalse(account.isEnabled());
    assertEquals("token-abc", account.getConfirmationToken());
    assertEquals(expiresAt, account.getConfirmationTokenExpiresAt());
  }

  @Test
  void confirmEmailShouldEnableAccountAndClearToken() {
    UserAccount account = newAccount();
    account.startEmailConfirmation("token-abc", LocalDateTime.now().plusHours(24));

    account.confirmEmail("token-abc");

    assertTrue(account.isEnabled());
    assertNull(account.getConfirmationToken());
    assertNull(account.getConfirmationTokenExpiresAt());
  }

  @Test
  void confirmEmailShouldRejectBlankOrMismatchedToken() {
    UserAccount account = newAccount();
    account.startEmailConfirmation("token-abc", LocalDateTime.now().plusHours(24));

    IllegalArgumentException blank = assertThrows(IllegalArgumentException.class, () -> account.confirmEmail(" "));
    assertEquals("Invalid confirmation token", blank.getMessage());

    IllegalArgumentException mismatch = assertThrows(IllegalArgumentException.class, () -> account.confirmEmail("other"));
    assertEquals("Invalid confirmation token", mismatch.getMessage());
  }

  @Test
  void confirmEmailShouldRejectExpiredToken() {
    UserAccount account = newAccount();
    account.startEmailConfirmation("token-abc", LocalDateTime.now().minusMinutes(1));

    IllegalArgumentException expired = assertThrows(IllegalArgumentException.class, () -> account.confirmEmail("token-abc"));
    assertEquals("Confirmation token expired", expired.getMessage());
  }

  private static UserAccount newAccount() {
    return new UserAccount(
        DomainID.generate(),
        new Email("user@bloodmatch.com"),
        "hash",
        Set.of());
  }
}
