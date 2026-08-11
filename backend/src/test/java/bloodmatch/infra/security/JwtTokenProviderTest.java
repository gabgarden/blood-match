package bloodmatch.infra.security;

import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import bloodmatch.infra.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

  @Test
  void shouldGenerateAndValidateTokenAndExtractClaims() {
    JwtTokenProvider tokenProvider = new JwtTokenProvider(createJwtProperties(60_000L));
    UserAccount userAccount = createUserAccount(Set.of(SecurityRole.DONOR, SecurityRole.REQUESTER));

    String token = tokenProvider.generateAccessToken(userAccount);

    assertTrue(tokenProvider.validateToken(token));
    assertEquals(userAccount.getId().getValue().toString(), tokenProvider.extractUserId(token));
    assertEquals(userAccount.getPartyId().getValue().toString(), tokenProvider.extractPartyId(token));
    assertEquals(List.of("DONOR", "REQUESTER"), tokenProvider.extractRoles(token).stream().sorted().toList());
  }

  @Test
  void shouldReturnFalseForTamperedToken() {
    JwtTokenProvider tokenProvider = new JwtTokenProvider(createJwtProperties(60_000L));
    UserAccount userAccount = createUserAccount(Set.of(SecurityRole.DONOR));

    String token = tokenProvider.generateAccessToken(userAccount);
    String tamperedToken = token.substring(0, token.length() - 2) + "aa";

    assertFalse(tokenProvider.validateToken(tamperedToken));
  }

  @Test
  void shouldReturnFalseForExpiredToken() {
    JwtTokenProvider tokenProvider = new JwtTokenProvider(createJwtProperties(-1_000L));
    UserAccount userAccount = createUserAccount(Set.of(SecurityRole.DONOR));

    String token = tokenProvider.generateAccessToken(userAccount);

    assertFalse(tokenProvider.validateToken(token));
  }

  private static JwtProperties createJwtProperties(long expirationMs) {
    JwtProperties jwtProperties = new JwtProperties();
    jwtProperties.setSecret("test-secret-key-with-at-least-32-characters");
    jwtProperties.setIssuer("bloodmatch-test");
    jwtProperties.setExpirationMs(expirationMs);
    return jwtProperties;
  }

  private static UserAccount createUserAccount(Set<SecurityRole> roles) {
    return new UserAccount(
        DomainID.generate(),
        new Email("jwt-test@bloodmatch.com"),
        "$2a$10$hashed-password",
        roles);
  }
}
