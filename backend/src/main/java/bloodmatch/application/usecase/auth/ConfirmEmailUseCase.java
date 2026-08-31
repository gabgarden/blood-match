package bloodmatch.application.usecase.auth;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.security.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmEmailUseCase {

  private final UserAccountRepositoryInterface userAccountRepository;

  public ConfirmEmailUseCase(UserAccountRepositoryInterface userAccountRepository) {
    if (userAccountRepository == null)
      throw new IllegalArgumentException("UserAccountRepository cannot be null");
    this.userAccountRepository = userAccountRepository;
  }

  @Transactional
  public Output execute(String token) {
    if (token == null || token.isBlank())
      throw new ValidationException("Invalid confirmation token");

    UserAccount userAccount = userAccountRepository.findByEmailConfirmationToken(token)
        .orElseThrow(() -> new ValidationException("Invalid confirmation token"));

    try {
      userAccount.confirmEmail(token);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(e.getMessage());
    }

    userAccountRepository.save(userAccount);
    return new Output("Email confirmed", userAccount.getEmail().getValue());
  }

  public record Output(String message, String email) {
  }
}
