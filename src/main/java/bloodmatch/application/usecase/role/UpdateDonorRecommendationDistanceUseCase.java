package bloodmatch.application.usecase.role;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateDonorRecommendationDistanceUseCase {

  private final DonorRepositoryInterface donorRepository;

  public UpdateDonorRecommendationDistanceUseCase(DonorRepositoryInterface donorRepository) {
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");

    this.donorRepository = donorRepository;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");
    if (input.maxDistanceInKm() <= 0)
      throw new ValidationException("maxDistanceInKm must be greater than zero");

    DomainID personId = DomainIdParser.parse(input.personId(), "personId");

    Donor donor = donorRepository.findByPartyId(personId)
        .orElseThrow(() -> new NotFoundException("Donor not found"));

    donor.updateMaxRecommendationDistanceKm(input.maxDistanceInKm());
    donorRepository.save(donor);
    return Output.from(donor);
  }

  public record Input(String personId, double maxDistanceInKm) {
  }

  public record Output(String personId, double maxDistanceInKm) {
    public static Output from(Donor donor) {
      return new Output(
          donor.getPerson().getId().getValue().toString(),
          donor.getMaxRecommendationDistanceKm());
    }
  }
}
