package bloodmatch.application.usecase.role;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateDonorProfileUseCase {

  private final DonorRepositoryInterface donorRepository;

  public UpdateDonorProfileUseCase(DonorRepositoryInterface donorRepository) {
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");

    this.donorRepository = donorRepository;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");
    if (input.bloodType() == null || input.bloodType().isBlank())
      throw new ValidationException("bloodType cannot be blank");

    DomainID personId = DomainIdParser.parse(input.personId(), "personId");
    BloodType bloodType = parseBloodType(input.bloodType());

    Donor donor = donorRepository.findByPartyId(personId)
        .orElseThrow(() -> new NotFoundException("Donor not found"));

    donor.updateProfile(bloodType, input.weight());
    donorRepository.save(donor);
    return Output.from(donor);
  }

  private BloodType parseBloodType(String bloodType) {
    try {
      return BloodType.of(bloodType);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(e.getMessage());
    }
  }

  public record Input(String personId, String bloodType, double weight) {
  }

  public record Output(String id) {
    public static Output from(Donor donor) {
      return new Output(donor.getId().getValue().toString());
    }
  }
}
