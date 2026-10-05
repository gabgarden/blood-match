package bloodmatch.shared.application.shared;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailConfirmationTokensTest {

  @Test
  void shouldGenerateHexToken() {
    String token = EmailConfirmationTokens.generate();
    assertNotNull(token);
    assertEquals(64, token.length()); // 32 bytes in hex = 64 characters
  }

  @Test
  void shouldComputeExpiresAt() {
    LocalDateTime now = LocalDateTime.of(2026, 4, 1, 10, 0);
    LocalDateTime expires = EmailConfirmationTokens.expiresAt(now);
    assertEquals(now.plusHours(24), expires);

    assertThrows(IllegalArgumentException.class, () -> EmailConfirmationTokens.expiresAt(null));
  }

  @Test
  void shouldFormatConfirmationLink() {
    String linkDefault = EmailConfirmationTokens.confirmationLink(null, "token123");
    assertEquals("http://localhost:5173/confirm-email?token=token123", linkDefault);

    String linkBlank = EmailConfirmationTokens.confirmationLink("  ", "token123");
    assertEquals("http://localhost:5173/confirm-email?token=token123", linkBlank);

    String linkCustom = EmailConfirmationTokens.confirmationLink("https://bloodmatch.org/", "token123");
    assertEquals("https://bloodmatch.org/confirm-email?token=token123", linkCustom);

    String linkWithoutTrailingSlash = EmailConfirmationTokens.confirmationLink("https://bloodmatch.org", "token123");
    assertEquals("https://bloodmatch.org/confirm-email?token=token123", linkWithoutTrailingSlash);
  }
}
