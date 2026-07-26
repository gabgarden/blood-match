package bloodmatch.application.usecase.party;

import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.repositories.PersonRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.Email;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.time.LocalDate;

@Service
public class RegisterPartyUseCase {

  private final PartyRepositoryInterface partyRepository;
  private final PersonRepositoryInterface personRepository;
  private final UserAccountRepositoryInterface userAccountRepository;
  private final PasswordEncoder passwordEncoder;

  public RegisterPartyUseCase(
      PartyRepositoryInterface partyRepository,
      PersonRepositoryInterface personRepository,
      UserAccountRepositoryInterface userAccountRepository,
      PasswordEncoder passwordEncoder) {

    if (partyRepository == null)
      throw new IllegalArgumentException("PartyRepository cannot be null");
    if (personRepository == null)
      throw new IllegalArgumentException("PersonRepository cannot be null");
    if (userAccountRepository == null)
      throw new IllegalArgumentException("UserAccountRepository cannot be null");
    if (passwordEncoder == null)
      throw new IllegalArgumentException("PasswordEncoder cannot be null");

    this.partyRepository = partyRepository;
    this.personRepository = personRepository;
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public Person registerPerson(
      String name,
      String cpf,
      LocalDate birthDate,
      String email,
      String password,
      String passwordConfirmation,
      String street,
      String city,
      String state,
      String zipCode) {

    validateCredentials(email, password, passwordConfirmation);
    Email userEmail = new Email(email);
    ensureEmailIsAvailable(userEmail);

    Person person = new Person(name, new CPF(cpf), birthDate);
    if (street != null && city != null && state != null && zipCode != null) {
      person.changeAddress(new Address(street, city, state, zipCode));
    }
    personRepository.save(person);

    UserAccount userAccount = new UserAccount(
        person.getId(),
        userEmail,
        passwordEncoder.encode(password),
        Collections.emptySet());
    userAccountRepository.save(userAccount);

    return person;
  }

  @Transactional
  public Organization registerOrganization(
      String name,
      String cnpj,
      String email,
      String password,
      String passwordConfirmation,
      String street,
      String city,
      String state,
      String zipCode) {

    validateCredentials(email, password, passwordConfirmation);
    Email userEmail = new Email(email);
    ensureEmailIsAvailable(userEmail);

    Organization organization = new Organization(name, new CNPJ(cnpj));
    if (street != null && city != null && state != null && zipCode != null) {
      organization.changeAddress(new Address(street, city, state, zipCode));
    }
    partyRepository.save(organization);

    UserAccount userAccount = new UserAccount(
        organization.getId(),
        userEmail,
        passwordEncoder.encode(password),
        Collections.emptySet());
    userAccountRepository.save(userAccount);

    return organization;
  }

  private void validateCredentials(String email, String password, String passwordConfirmation) {
    if (email == null || email.isBlank())
      throw new IllegalArgumentException("email cannot be blank");
    if (password == null || password.isBlank())
      throw new IllegalArgumentException("password cannot be blank");
    if (password.length() < 8)
      throw new IllegalArgumentException("password must be at least 8 characters");
    if (passwordConfirmation == null || passwordConfirmation.isBlank())
      throw new IllegalArgumentException("passwordConfirmation cannot be blank");
    if (!password.equals(passwordConfirmation))
      throw new IllegalArgumentException("password and passwordConfirmation must match");
  }

  private void ensureEmailIsAvailable(Email email) {
    if (userAccountRepository.findByEmail(email).isPresent())
      throw new IllegalStateException("Email already registered");
  }
}
