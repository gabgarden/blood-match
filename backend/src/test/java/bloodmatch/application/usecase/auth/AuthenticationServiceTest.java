package bloodmatch.application.usecase.auth;

import bloodmatch.application.exception.UnauthorizedException;
import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import bloodmatch.infra.config.JwtProperties;
import bloodmatch.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

  private final UserAccountRepositoryInterface userAccountRepository = mock(UserAccountRepositoryInterface.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);

  private final JwtProperties jwtProperties = new JwtProperties();

  private final AuthenticationService authenticationService;

  AuthenticationServiceTest() {
    jwtProperties.setExpirationMs(3600_000L);
    jwtProperties.setIssuer("bloodmatch-test");
    jwtProperties.setSecret("test-secret-key-with-at-least-32-characters");

    this.authenticationService = new AuthenticationService(
        userAccountRepository,
        passwordEncoder,
        jwtTokenProvider,
        jwtProperties);
  }

  @Test
  void shouldAuthenticateWhenCredentialsAreValid() {
    UserAccount userAccount = createUserAccount();

    when(userAccountRepository.findByEmail(new Email("user@bloodmatch.com")))
        .thenReturn(Optional.of(userAccount));
    when(passwordEncoder.matches("plain-password", userAccount.getPasswordHash()))
        .thenReturn(true);
    when(jwtTokenProvider.generateAccessToken(userAccount)).thenReturn("valid-token");

    AuthenticationService.Output result = authenticationService.authenticate(
        "user@bloodmatch.com",
        "plain-password");

    assertEquals("valid-token", result.accessToken());
    assertEquals("Bearer", result.tokenType());
    assertEquals(3600_000L, result.expiresIn());
    assertEquals(userAccount.getPartyId().getValue().toString(), result.partyId());
    assertEquals(Set.of("DONOR"), result.roles());
  }

  @Test
  void shouldThrowWhenPasswordIsInvalid() {
    UserAccount userAccount = createUserAccount();

    when(userAccountRepository.findByEmail(new Email("user@bloodmatch.com")))
        .thenReturn(Optional.of(userAccount));
    when(passwordEncoder.matches("wrong-password", userAccount.getPasswordHash()))
        .thenReturn(false);

    assertThrows(UnauthorizedException.class,
        () -> authenticationService.authenticate("user@bloodmatch.com", "wrong-password"));
  }

  @Test
  void shouldThrowWhenUserIsDisabled() {
    UserAccount userAccount = createUserAccount();
    userAccount.disable();

    when(userAccountRepository.findByEmail(new Email("user@bloodmatch.com")))
        .thenReturn(Optional.of(userAccount));

    assertThrows(UnauthorizedException.class,
        () -> authenticationService.authenticate("user@bloodmatch.com", "plain-password"));
  }

  @Test
  void shouldThrowWhenEmailIsNotRegistered() {
    when(userAccountRepository.findByEmail(new Email("missing@bloodmatch.com")))
        .thenReturn(Optional.empty());

    assertThrows(UnauthorizedException.class,
        () -> authenticationService.authenticate("missing@bloodmatch.com", "plain-password"));
  }

  private static UserAccount createUserAccount() {
    DomainID partyId = DomainID.generate();
    return new UserAccount(
        partyId,
        new Email("user@bloodmatch.com"),
        "$2a$10$hashed-password",
        Set.of(SecurityRole.DONOR));
  }
}
