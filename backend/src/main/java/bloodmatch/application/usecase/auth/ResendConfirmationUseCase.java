package bloodmatch.application.usecase.auth;

import bloodmatch.application.shared.EmailConfirmationTokens;
import bloodmatch.domain.party.Party;
import bloodmatch.domain.party.PartyRepositoryInterface;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.Email;
import bloodmatch.infra.external.notification.EmailConfirmationMailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ResendConfirmationUseCase {

  static final String RESPONSE_MESSAGE =
      "If the email is registered and pending confirmation, a new message was sent.";

  private final UserAccountRepositoryInterface userAccountRepository;
  private final PartyRepositoryInterface partyRepository;
  private final EmailConfirmationMailer emailConfirmationMailer;
  private final String publicUrl;

  public ResendConfirmationUseCase(
      UserAccountRepositoryInterface userAccountRepository,
      PartyRepositoryInterface partyRepository,
      EmailConfirmationMailer emailConfirmationMailer,
      @Value("${app.public-url:http://localhost:5173}") String publicUrl) {

    if (userAccountRepository == null)
      throw new IllegalArgumentException("UserAccountRepository cannot be null");
    if (partyRepository == null)
      throw new IllegalArgumentException("PartyRepository cannot be null");
    if (emailConfirmationMailer == null)
      throw new IllegalArgumentException("EmailConfirmationMailer cannot be null");

    this.userAccountRepository = userAccountRepository;
    this.partyRepository = partyRepository;
    this.emailConfirmationMailer = emailConfirmationMailer;
    this.publicUrl = publicUrl;
  }

  @Transactional
  public Output execute(String email) {
    try {
      userAccountRepository.findByEmail(new Email(email)).ifPresent(this::rotateAndSendIfPending);
    } catch (IllegalArgumentException ignored) {
      // Invalid email format must not leak whether an account exists.
    }

    return new Output(RESPONSE_MESSAGE);
  }

  private void rotateAndSendIfPending(UserAccount userAccount) {
    if (userAccount.isEnabled())
      return;
    if (userAccount.getConfirmationToken() == null)
      return;

    String token = EmailConfirmationTokens.generate();
    userAccount.startEmailConfirmation(token, EmailConfirmationTokens.expiresAt(LocalDateTime.now()));
    userAccountRepository.save(userAccount);

    String recipientName = partyRepository.findById(userAccount.getPartyId())
        .map(Party::getName)
        .orElse(null);
    emailConfirmationMailer.sendConfirmationEmail(
        userAccount.getEmail().getValue(),
        recipientName,
        EmailConfirmationTokens.confirmationLink(publicUrl, token));
  }

  public record Output(String message) {
  }
}
