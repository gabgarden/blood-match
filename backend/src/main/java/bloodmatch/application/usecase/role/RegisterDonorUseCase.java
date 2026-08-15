package bloodmatch.application.usecase.role;

import bloodmatch.application.exception.ConflictException;
import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.party.PersonRepositoryInterface;
import bloodmatch.domain.security.UserAccountRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.services.GeocodingServiceInterface;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Service
public class RegisterDonorUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final PersonRepositoryInterface personRepository;
  private final UserAccountRepositoryInterface userAccountRepository;
  private final GeocodingServiceInterface geocodingService;

  public RegisterDonorUseCase(
      DonorRepositoryInterface donorRepository,
      PersonRepositoryInterface personRepository,
      UserAccountRepositoryInterface userAccountRepository,
      GeocodingServiceInterface geocodingService) {

    if (donorRepository == null) throw new IllegalArgumentException("DonorRepository cannot be null");
    if (personRepository == null) throw new IllegalArgumentException("PersonRepository cannot be null");
    if (userAccountRepository == null) throw new IllegalArgumentException("UserAccountRepository cannot be null");
    if (geocodingService == null) throw new IllegalArgumentException("GeocodingService cannot be null");

    this.donorRepository = donorRepository;
    this.personRepository = personRepository;
    this.userAccountRepository = userAccountRepository;
    this.geocodingService = geocodingService;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");
    if (input.bloodType() == null || input.bloodType().isBlank())
      throw new ValidationException("bloodType cannot be blank");

    DomainID personId = DomainIdParser.parse(input.personId(), "personId");
    BloodType bloodType = parseBloodType(input.bloodType());

    Person person = personRepository.findById(personId)
        .orElseThrow(() -> new NotFoundException("Person not found"));

    if (donorRepository.findByPartyId(personId).isPresent()) {
      throw new ConflictException("Donor already registered for person");
    }

    Address currentAddress = person.getAddress();
    if (currentAddress != null && !currentAddress.hasCoordinates()) {
      Address addressWithCoords = geocodingService.getCoordinatesFromAddress(currentAddress);
      person.changeAddress(addressWithCoords);
      personRepository.save(person);
    }

    Donor donor;
    try {
      donor = new Donor(person, bloodType, input.weight());
      if (input.lastDonationDate() != null) {
        donor.registerDonation(input.lastDonationDate());
      }
    } catch (IllegalArgumentException e) {
      throw new ValidationException(e.getMessage());
    }
    donorRepository.save(donor);
    addRoleToUserAccount(personId, SecurityRole.DONOR);
    return Output.from(donor);
  }

  private BloodType parseBloodType(String bloodType) {
    try {
      return BloodType.of(bloodType);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(e.getMessage());
    }
  }

  private void addRoleToUserAccount(DomainID partyId, SecurityRole role) {
    UserAccount userAccount = userAccountRepository.findByPartyId(partyId)
        .orElseThrow(() -> new NotFoundException("User account not found for party"));

    Set<SecurityRole> updatedRoles = new HashSet<>(userAccount.getRoles());
    updatedRoles.add(role);
    userAccount.updateRoles(updatedRoles);
    userAccountRepository.save(userAccount);
  }

  public record Input(String personId, String bloodType, double weight, LocalDate lastDonationDate) {
  }

  public record Output(String id) {
    public static Output from(Donor donor) {
      return new Output(donor.getId().getValue().toString());
    }
  }
}
