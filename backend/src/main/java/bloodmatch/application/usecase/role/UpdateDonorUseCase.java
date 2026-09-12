package bloodmatch.application.usecase.role;

import bloodmatch.application.exception.ConcurrencyException;
import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
public class UpdateDonorUseCase {

  private final DonorRepositoryInterface donorRepository;

  public UpdateDonorUseCase(DonorRepositoryInterface donorRepository) {
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");

    this.donorRepository = donorRepository;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");
    if (input.version() == null)
      throw new ValidationException("version cannot be null");

    if (input.bloodType() == null && input.weight() == null && input.maxDistanceInKm() == null) {
      throw new ValidationException("At least one mutable field must be provided");
    }

    DomainID personId = DomainIdParser.parse(input.personId(), "personId");

    Donor donor = donorRepository.findByPartyId(personId)
        .orElseThrow(() -> new NotFoundException("Donor not found"));

    if (!Objects.equals(donor.getVersion(), input.version())) {
      throw new ConcurrencyException("Resource version conflict: expected " + input.version() + " but found " + donor.getVersion());
    }

    if (input.bloodType() != null) {
      BloodType bloodType = parseBloodType(input.bloodType());
      donor.updateBloodType(bloodType);
    }

    if (input.weight() != null) {
      try {
        donor.updateWeight(input.weight());
      } catch (IllegalArgumentException e) {
         throw new ValidationException(e.getMessage());
      }
    }

    if (input.maxDistanceInKm() != null) {
      try {
        donor.updateMaxRecommendationDistanceKm(input.maxDistanceInKm());
      } catch (IllegalArgumentException e) {
         throw new ValidationException(e.getMessage());
      }
    }

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

  public record Input(String personId, Long version, String bloodType, Double weight, Double maxDistanceInKm) {
  }

  public record Output(String id, String personId, Long version, String bloodType, Double weight, LocalDate weightUpdatedAt, Double maxDistanceInKm) {
    public static Output from(Donor donor) {
      return new Output(
          donor.getId().getValue().toString(),
          donor.getPerson().getId().getValue().toString(),
          donor.getVersion(),
          donor.getBloodType().getType(),
          donor.getWeight(),
          donor.getWeightUpdatedAt(),
          donor.getMaxRecommendationDistanceKm());
    }
  }
}
