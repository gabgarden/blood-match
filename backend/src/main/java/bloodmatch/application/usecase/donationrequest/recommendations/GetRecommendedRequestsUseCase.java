package bloodmatch.application.usecase.donationrequest.recommendations;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class GetRecommendedRequestsUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final DonationRequestRepositoryInterface donationRequestRepository;

  public GetRecommendedRequestsUseCase(
      DonorRepositoryInterface donorRepository,
      DonationRequestRepositoryInterface donationRequestRepository) {
    this.donorRepository = donorRepository;
    this.donationRequestRepository = donationRequestRepository;
  }

  public List<OutputItem> execute(Input input) {
    return execute(input, LocalDate.now());
  }

  public List<OutputItem> execute(Input input, LocalDate currentDate) {
    if (input == null) {
      throw new ValidationException("Input cannot be null");
    }
    if (currentDate == null) {
      throw new ValidationException("Current date cannot be null");
    }

    DomainID personId = DomainIdParser.parse(input.personId(), "personId");

    Donor donor = donorRepository.findByPartyId(personId)
        .orElseThrow(() -> new NotFoundException("Donor role not found"));

    if (!donor.isEligibleToDonate(currentDate)) {
      return List.of();
    }

    List<DonationRequest> candidateRequests = donationRequestRepository.findActiveRequestsForDonor(
        donor.getBloodType(),
        donor.getPerson().getAddress(),
        donor.getMaxRecommendationDistanceKm(),
        currentDate);

    if (candidateRequests.isEmpty()) {
      return List.of();
    }

    return candidateRequests.stream()
        .filter(request -> !request.isGoalReached())
        .sorted(
            Comparator
                .comparing((DonationRequest request) -> distanceKm(donor, request),
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(DonationRequest::getUrgency, Comparator.reverseOrder())
                .thenComparing(DonationRequest::getDateLimit))
        .map(request -> toOutput(request, donor))
        .toList();
  }

  private OutputItem toOutput(DonationRequest request, Donor donor) {
    Double distance = distanceKm(donor, request);
    Address address = request.getBloodCenter().getOrganization().getAddress();
    Double latitude = address != null ? address.getLatitude() : null;
    Double longitude = address != null ? address.getLongitude() : null;

    return new OutputItem(
        request.getId().getValue().toString(),
        request.getBloodCenter().getOrganization().getId().getValue().toString(),
        request.getBloodTypeNeeded().getType(),
        request.getDateLimit(),
        request.getBloodCenter().getOrganization().getName(),
        request.getUrgency().name(),
        distance != null ? Math.round(distance * 10.0) / 10.0 : null,
        request.getGoalBloodBags(),
        request.getFulfilledBloodBags(),
        request.isGoalReached(),
        latitude,
        longitude);
  }

  private static Double distanceKm(Donor donor, DonationRequest request) {
    Address addr1 = donor.getPerson().getAddress();
    Address addr2 = request.getBloodCenter().getOrganization().getAddress();
    if (addr1 == null || addr2 == null) {
      return null;
    }
    return addr1.distanceTo(addr2);
  }

  public record Input(String personId) {
  }

  public record OutputItem(
      String requestId,
      String organizationId,
      String bloodTypeNeeded,
      LocalDate dateLimit,
      String bloodCenterName,
      String urgency,
      Double distanceInKm,
      int goalBloodBags,
      int fulfilledBloodBags,
      boolean goalReached,
      Double latitude,
      Double longitude) {
  }
}
