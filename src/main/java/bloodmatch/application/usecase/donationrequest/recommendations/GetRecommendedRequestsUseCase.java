package bloodmatch.application.usecase.donationrequest.recommendations;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GetRecommendedRequestsUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRepositoryInterface donationRepository;

  public GetRecommendedRequestsUseCase(
      DonorRepositoryInterface donorRepository,
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRepositoryInterface donationRepository) {
    this.donorRepository = donorRepository;
    this.donationRequestRepository = donationRequestRepository;
    this.donationRepository = donationRepository;
  }

  public List<OutputItem> execute(DomainID donorId) {
    return execute(donorId, LocalDate.now());
  }

  public List<OutputItem> execute(DomainID donorId, LocalDate currentDate) {
    if (donorId == null) throw new IllegalArgumentException("Donor id cannot be null");
    if (currentDate == null) throw new IllegalArgumentException("Current date cannot be null");

    Donor donor = donorRepository.findByPartyId(donorId)
        .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));

    return donationRequestRepository.findActiveRequests()
        .stream()
        .filter(request -> donor.isEligibleToDonate(currentDate))
        .filter(request -> request.canBeFulfilledBy(donor.getBloodType(), currentDate))
        .filter(request -> !donationRepository.existsByDonorIdAndRequestId(donor.getPerson().getId(), request.getId()))
        .map(request -> toOutput(request, donor))

        // Ordena primeiro pela distância (mais perto) e depois pela data limite
        .sorted(java.util.Comparator
            .comparing(OutputItem::distanceInKm, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))
            .thenComparing(OutputItem::dateLimit))

        .toList();
  }

  private OutputItem toOutput(DonationRequest request, Donor donor) {
    Double distance = calculateDistance(donor.getPerson().getAddress(), request.getBloodCenter().getOrganization().getAddress());

    return new OutputItem(
        request.getId().getValue().toString(),
        request.getBloodTypeNeeded().getType(),
        request.getDateLimit(),
        request.getBloodCenter().getOrganization().getName(),
        request.getUrgency(),
        distance != null ? Math.round(distance * 10.0) / 10.0 : null 
    );
  }

  // Fórmula de Haversine para calcular a distância física em Km usando Latitude e Longitude
  private Double calculateDistance(Address addr1, Address addr2) {
      if (addr1 == null || addr2 == null || !addr1.hasCoordinates() || !addr2.hasCoordinates()) {
          return null;
      }

      final int R = 6371; // Raio médio da Terra em Km
      double latDistance = Math.toRadians(addr2.getLatitude() - addr1.getLatitude());
      double lonDistance = Math.toRadians(addr2.getLongitude() - addr1.getLongitude());
      
      double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
              + Math.cos(Math.toRadians(addr1.getLatitude())) * Math.cos(Math.toRadians(addr2.getLatitude()))
              * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
      
      double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      return R * c;
  }

  public record OutputItem(
      String requestId,
      String bloodTypeNeeded,
      java.time.LocalDate dateLimit,
      String bloodCenterName,
      Urgency urgency,
      Double distanceInKm 
  ) {}
}