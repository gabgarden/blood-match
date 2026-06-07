package bloodmatch.application.usecase.role;

import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.repositories.PersonRepositoryInterface;
import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.services.GeocodingServiceInterface;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  public Donor execute(DomainID personId, BloodType bloodType, double weight) {

    if (personId == null) throw new IllegalArgumentException("Person id cannot be null");
    if (bloodType == null) throw new IllegalArgumentException("Blood type cannot be null");

    Person person = personRepository.findById(personId)
        .orElseThrow(() -> new IllegalArgumentException("Person not found"));

    if (donorRepository.findByPartyId(personId).isPresent()) {
      throw new IllegalStateException("Donor already registered for person");
    }

    //geo
    Address currentAddress = person.getAddress();
    if (currentAddress != null && !currentAddress.hasCoordinates()) {
        Address addressWithCoords = geocodingService.getCoordinatesFromAddress(currentAddress);
        person.changeAddress(addressWithCoords);
        personRepository.save(person);
    }

    Donor donor = new Donor(person, bloodType, weight);
    donorRepository.save(donor);
    addRoleToUserAccount(personId, SecurityRole.DONOR);
    return donor;
  }

  private void addRoleToUserAccount(DomainID partyId, SecurityRole role) {
    UserAccount userAccount = userAccountRepository.findByPartyId(partyId)
        .orElseThrow(() -> new IllegalStateException("User account not found for party"));

    Set<SecurityRole> updatedRoles = new HashSet<>(userAccount.getRoles());
    updatedRoles.add(role);
    userAccount.updateRoles(updatedRoles);
    userAccountRepository.save(userAccount);
  }
}