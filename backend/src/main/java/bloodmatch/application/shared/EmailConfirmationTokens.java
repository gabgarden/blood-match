package bloodmatch.application.shared;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

public final class EmailConfirmationTokens {

  public static final int EXPIRY_HOURS = 24;
  private static final int TOKEN_BYTES = 32;
  private static final SecureRandom RANDOM = new SecureRandom();

  private EmailConfirmationTokens() {
  }

  public static String generate() {
    byte[] bytes = new byte[TOKEN_BYTES];
    RANDOM.nextBytes(bytes);
    return HexFormat.of().formatHex(bytes);
  }

  public static LocalDateTime expiresAt(LocalDateTime now) {
    if (now == null)
      throw new IllegalArgumentException("now cannot be null");
    return now.plusHours(EXPIRY_HOURS);
  }

  public static String confirmationLink(String publicUrl, String token) {
    String base = publicUrl == null || publicUrl.isBlank()
        ? "http://localhost:5173"
        : publicUrl.strip();
    if (base.endsWith("/"))
      base = base.substring(0, base.length() - 1);
    return base + "/confirm-email?token=" + token;
  }
}
