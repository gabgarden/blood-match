package bloodmatch.application.usecase.party;

import bloodmatch.application.exception.ConflictException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.EmailConfirmationTokens;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.party.PartyRepositoryInterface;
import bloodmatch.domain.party.PersonRepositoryInterface;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.Email;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import bloodmatch.infra.external.notification.EmailConfirmationMailer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class RegisterPartyUseCase {

  private final PartyRepositoryInterface partyRepository;
  private final PersonRepositoryInterface personRepository;
  private final UserAccountRepositoryInterface userAccountRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailConfirmationMailer emailConfirmationMailer;
  private final boolean requireEmailConfirmation;
  private final String publicUrl;

  public RegisterPartyUseCase(
      PartyRepositoryInterface partyRepository,
      PersonRepositoryInterface personRepository,
      UserAccountRepositoryInterface userAccountRepository,
      PasswordEncoder passwordEncoder,
      EmailConfirmationMailer emailConfirmationMailer,
      @Value("${app.require-email-confirmation:false}") boolean requireEmailConfirmation,
      @Value("${app.public-url:http://localhost:5173}") String publicUrl) {

    if (partyRepository == null)
      throw new IllegalArgumentException("PartyRepository cannot be null");
    if (personRepository == null)
      throw new IllegalArgumentException("PersonRepository cannot be null");
    if (userAccountRepository == null)
      throw new IllegalArgumentException("UserAccountRepository cannot be null");
    if (passwordEncoder == null)
      throw new IllegalArgumentException("PasswordEncoder cannot be null");
    if (emailConfirmationMailer == null)
      throw new IllegalArgumentException("EmailConfirmationMailer cannot be null");

    this.partyRepository = partyRepository;
    this.personRepository = personRepository;
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailConfirmationMailer = emailConfirmationMailer;
    this.requireEmailConfirmation = requireEmailConfirmation;
    this.publicUrl = publicUrl;
  }

  @Transactional
  public Output registerPerson(PersonInput input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");

    validateCredentials(input.email(), input.password(), input.passwordConfirmation());
    Email userEmail = new Email(input.email());
    ensureEmailIsAvailable(userEmail);

    Person person = new Person(
        input.name(),
        new PhoneNumber(input.phoneNumber()),
        new CPF(input.cpf()),
        input.birthDate());
    if (input.street() != null && input.city() != null && input.state() != null && input.zipCode() != null) {
      person.changeAddress(new Address(input.street(), input.city(), input.state(), input.zipCode()));
    }
    personRepository.save(person);

    UserAccount userAccount = new UserAccount(
        person.getId(),
        userEmail,
        passwordEncoder.encode(input.password()),
        Collections.emptySet());
    persistAccount(userAccount, person.getName());

    return Output.from(person, requireEmailConfirmation);
  }

  @Transactional
  public Output registerOrganization(OrganizationInput input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");

    validateCredentials(input.email(), input.password(), input.passwordConfirmation());
    Email userEmail = new Email(input.email());
    ensureEmailIsAvailable(userEmail);

    Organization organization = new Organization(
        input.name(),
        new PhoneNumber(input.phoneNumber()),
        new CNPJ(input.cnpj()));
    if (input.street() != null && input.city() != null && input.state() != null && input.zipCode() != null) {
      organization.changeAddress(new Address(input.street(), input.city(), input.state(), input.zipCode()));
    }
    partyRepository.save(organization);

    UserAccount userAccount = new UserAccount(
        organization.getId(),
        userEmail,
        passwordEncoder.encode(input.password()),
        Collections.emptySet());
    persistAccount(userAccount, organization.getName());

    return Output.from(organization, requireEmailConfirmation);
  }

  private void persistAccount(UserAccount userAccount, String recipientName) {
    userAccountRepository.save(userAccount);

    if (!requireEmailConfirmation)
      return;

    String token = EmailConfirmationTokens.generate();
    userAccount.startEmailConfirmation(token, EmailConfirmationTokens.expiresAt(LocalDateTime.now()));
    userAccountRepository.save(userAccount);
    emailConfirmationMailer.sendConfirmationEmail(
        userAccount.getEmail().getValue(),
        recipientName,
        EmailConfirmationTokens.confirmationLink(publicUrl, token));
  }

  private void validateCredentials(String email, String password, String passwordConfirmation) {
    if (email == null || email.isBlank())
      throw new ValidationException("email cannot be blank");
    if (password == null || password.isBlank())
      throw new ValidationException("password cannot be blank");
    if (password.length() < 8)
      throw new ValidationException("password must be at least 8 characters");
    if (passwordConfirmation == null || passwordConfirmation.isBlank())
      throw new ValidationException("passwordConfirmation cannot be blank");
    if (!password.equals(passwordConfirmation))
      throw new ValidationException("password and passwordConfirmation must match");
  }

  private void ensureEmailIsAvailable(Email email) {
    if (userAccountRepository.findByEmail(email).isPresent())
      throw new ConflictException("Email already registered");
  }

  public record PersonInput(
      String name,
      String phoneNumber,
      String cpf,
      LocalDate birthDate,
      String email,
      String password,
      String passwordConfirmation,
      String street,
      String city,
      String state,
      String zipCode) {
  }

  public record OrganizationInput(
      String name,
      String phoneNumber,
      String cnpj,
      String email,
      String password,
      String passwordConfirmation,
      String street,
      String city,
      String state,
      String zipCode) {
  }

  public record Output(String id, String type, boolean emailConfirmationRequired) {
    public static Output from(Person person, boolean emailConfirmationRequired) {
      return new Output(person.getId().getValue().toString(), "PERSON", emailConfirmationRequired);
    }

    public static Output from(Organization organization, boolean emailConfirmationRequired) {
      return new Output(organization.getId().getValue().toString(), "ORGANIZATION", emailConfirmationRequired);
    }
  }
}
