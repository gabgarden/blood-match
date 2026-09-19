package bloodmatch.role.application;

import bloodmatch.shared.application.exception.ConflictException;
import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.application.shared.DomainIdParser;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.party.domain.PersonRepositoryInterface;
import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.auth.domain.SecurityRole;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.shared.domain.services.GeocodingServiceInterface;
import bloodmatch.shared.domain.valueObjects.Address;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.DomainID;
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
