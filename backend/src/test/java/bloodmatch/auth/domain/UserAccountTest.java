package bloodmatch.auth.domain;

import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.Email;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
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

  @Test
  void confirmEmailShouldRejectWhenNoTokenPendingOrExpiresAtNull() {
    UserAccount account = newAccount();
    assertThrows(IllegalArgumentException.class, () -> account.confirmEmail("token-abc"));

    // Account rehydrated with token but null expiresAt
    UserAccount rehydrated = UserAccount.rehydrate(
        DomainID.generate(),
        DomainID.generate(),
        new Email("user@bloodmatch.com"),
        "hash",
        Set.of(),
        false,
        LocalDateTime.now(),
        LocalDateTime.now(),
        "token-123",
        null);
    assertThrows(IllegalArgumentException.class, () -> rehydrated.confirmEmail("token-123"));
  }

  @Test
  void shouldRejectNullOrBlankConstructorArguments() {
    DomainID partyId = DomainID.generate();
    Email email = new Email("user@bloodmatch.com");

    assertThrows(IllegalArgumentException.class, () -> new UserAccount(null, email, "hash", Set.of()));
    assertThrows(IllegalArgumentException.class, () -> new UserAccount(partyId, null, "hash", Set.of()));
    assertThrows(IllegalArgumentException.class, () -> new UserAccount(partyId, email, null, Set.of()));
    assertThrows(IllegalArgumentException.class, () -> new UserAccount(partyId, email, "   ", Set.of()));

    Set<SecurityRole> withNull = new HashSet<>();
    withNull.add(null);
    assertThrows(IllegalArgumentException.class, () -> new UserAccount(partyId, email, "hash", withNull));

    // Null roles set creates empty set
    UserAccount acc = new UserAccount(partyId, email, "hash", null);
    assertTrue(acc.getRoles().isEmpty());
  }

  @Test
  void shouldValidateStartEmailConfirmationArguments() {
    UserAccount account = newAccount();
    assertThrows(IllegalArgumentException.class, () -> account.startEmailConfirmation(null, LocalDateTime.now().plusDays(1)));
    assertThrows(IllegalArgumentException.class, () -> account.startEmailConfirmation("", LocalDateTime.now().plusDays(1)));
    assertThrows(IllegalArgumentException.class, () -> account.startEmailConfirmation("token", null));
  }

  @Test
  void shouldValidateUpdatePasswordHashAndRoles() {
    UserAccount account = newAccount();

    assertThrows(IllegalArgumentException.class, () -> account.updatePasswordHash(null));
    assertThrows(IllegalArgumentException.class, () -> account.updatePasswordHash(""));

    account.updatePasswordHash("newHash");
    assertEquals("newHash", account.getPasswordHash());

    Set<SecurityRole> withNull = new HashSet<>();
    withNull.add(null);
    assertThrows(IllegalArgumentException.class, () -> account.updateRoles(withNull));

    account.updateRoles(Set.of(SecurityRole.DONOR));
    assertEquals(Set.of(SecurityRole.DONOR), account.getRoles());

    account.updateRoles(null);
    assertTrue(account.getRoles().isEmpty());
  }

  @Test
  void shouldEnableAndDisableAccount() {
    UserAccount account = newAccount();
    assertTrue(account.isEnabled());

    account.disable();
    assertFalse(account.isEnabled());

    account.enable();
    assertTrue(account.isEnabled());
  }

  @Test
  void shouldClearConfirmationToken() {
    UserAccount account = newAccount();
    account.startEmailConfirmation("token-xyz", LocalDateTime.now().plusHours(12));
    assertEquals("token-xyz", account.getConfirmationToken());

    account.clearConfirmationToken();
    assertNull(account.getConfirmationToken());
    assertNull(account.getConfirmationTokenExpiresAt());
  }

  @Test
  void shouldSupportRehydrateAndValidateArguments() {
    DomainID id = DomainID.generate();
    DomainID partyId = DomainID.generate();
    Email email = new Email("user@bloodmatch.com");
    LocalDateTime now = LocalDateTime.now();

    assertThrows(IllegalArgumentException.class, () -> UserAccount.rehydrate(
        null, partyId, email, "hash", Set.of(), true, now, now, null, null));
    assertThrows(IllegalArgumentException.class, () -> UserAccount.rehydrate(
        id, partyId, email, "hash", Set.of(), true, null, now, null, null));
    assertThrows(IllegalArgumentException.class, () -> UserAccount.rehydrate(
        id, partyId, email, "hash", Set.of(), true, now, null, null, null));

    UserAccount rehydrated = UserAccount.rehydrate(
        id, partyId, email, "hash", Set.of(SecurityRole.DONOR), true, now, now, "token", now.plusDays(1), 5L);

    assertEquals(id, rehydrated.getId());
    assertEquals(partyId, rehydrated.getPartyId());
    assertEquals(email, rehydrated.getEmail());
    assertEquals("hash", rehydrated.getPasswordHash());
    assertEquals(Set.of(SecurityRole.DONOR), rehydrated.getRoles());
    assertTrue(rehydrated.isEnabled());
    assertEquals(now, rehydrated.getCreatedAt());
    assertEquals(now, rehydrated.getUpdatedAt());
    assertEquals("token", rehydrated.getConfirmationToken());
    assertEquals(now.plusDays(1), rehydrated.getConfirmationTokenExpiresAt());
    assertEquals(5L, rehydrated.getVersion());
  }

  private static UserAccount newAccount() {
    return new UserAccount(
        DomainID.generate(),
        new Email("user@bloodmatch.com"),
        "hash",
        Set.of());
  }
}
