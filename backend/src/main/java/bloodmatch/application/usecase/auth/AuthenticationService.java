package bloodmatch.application.usecase.auth;

import bloodmatch.application.exception.UnauthorizedException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.Email;
import bloodmatch.infra.config.JwtProperties;
import bloodmatch.infra.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

  private final UserAccountRepositoryInterface userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtProperties jwtProperties;

  public AuthenticationService(
      UserAccountRepositoryInterface userAccountRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenProvider jwtTokenProvider,
      JwtProperties jwtProperties) {

    if (userAccountRepository == null) {
      throw new IllegalArgumentException("UserAccountRepository cannot be null");
    }
    if (passwordEncoder == null) {
      throw new IllegalArgumentException("PasswordEncoder cannot be null");
    }
    if (jwtTokenProvider == null) {
      throw new IllegalArgumentException("JwtTokenProvider cannot be null");
    }
    if (jwtProperties == null) {
      throw new IllegalArgumentException("JwtProperties cannot be null");
    }

    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenProvider = jwtTokenProvider;
    this.jwtProperties = jwtProperties;
  }

  public Output authenticate(String email, String password) {
    if (email == null || email.isBlank()) {
      throw new ValidationException("email cannot be blank");
    }
    if (password == null || password.isBlank()) {
      throw new ValidationException("password cannot be blank");
    }

    UserAccount userAccount = userAccountRepository.findByEmail(new Email(email))
        .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

    if (!userAccount.isEnabled()) {
      throw new UnauthorizedException("User account is disabled");
    }

    if (!passwordEncoder.matches(password, userAccount.getPasswordHash())) {
      throw new UnauthorizedException("Invalid credentials");
    }

    String accessToken = jwtTokenProvider.generateAccessToken(userAccount);

    Set<String> roles = userAccount.getRoles().stream()
        .map(SecurityRole::name)
        .collect(Collectors.toSet());

    return new Output(
        accessToken,
        "Bearer",
        jwtProperties.getExpirationMs(),
        roles,
        userAccount.getPartyId().getValue().toString());
  }

  public record Output(
      String accessToken,
      String tokenType,
      long expiresIn,
      Set<String> roles,
      String partyId) {
  }
}
