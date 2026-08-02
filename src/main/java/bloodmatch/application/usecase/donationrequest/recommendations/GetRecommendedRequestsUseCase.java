package bloodmatch.application.usecase.donationrequest.recommendations;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
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

    public List<OutputItem> execute(DomainID personId) {
        return execute(personId, LocalDate.now());
    }

    public List<OutputItem> execute(DomainID personId, LocalDate currentDate) {

        if (personId == null)
            throw new IllegalArgumentException("Person id cannot be null");

        if (currentDate == null)
            throw new IllegalArgumentException("Current date cannot be null");

        Donor donor = donorRepository.findByPartyId(personId)
                .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));

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
                .map(request -> toOutput(
                        request,
                        donor))
                .sorted(
                        Comparator
                                .comparing(
                                        OutputItem::distanceInKm,
                                        Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(OutputItem::urgency, Comparator.reverseOrder())
                                .thenComparing(OutputItem::dateLimit))
                .toList();
    }

    private OutputItem toOutput(
            DonationRequest request,
            Donor donor) {

        Double distance = calculateDistance(
                donor.getPerson().getAddress(),
                request.getBloodCenter().getOrganization().getAddress());

        return new OutputItem(
                request.getId().getValue().toString(),
                request.getBloodTypeNeeded().getType(),
                request.getDateLimit(),
                request.getBloodCenter().getOrganization().getName(),
                request.getUrgency(),
                distance != null ? Math.round(distance * 10.0) / 10.0 : null,
                request.getGoalBloodBags(),
                request.getFulfilledBloodBags(),
                request.isGoalReached());
    }

    private Double calculateDistance(Address addr1, Address addr2) {
        if (addr1 == null || addr2 == null
                || !addr1.hasCoordinates()
                || !addr2.hasCoordinates()) {
            return null;
        }

        final int R = 6371;

        double latDistance = Math.toRadians(
                addr2.getLatitude() - addr1.getLatitude());

        double lonDistance = Math.toRadians(
                addr2.getLongitude() - addr1.getLongitude());

        double a =
                Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(addr1.getLatitude()))
                        * Math.cos(Math.toRadians(addr2.getLatitude()))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public record OutputItem(
            String requestId,
            String bloodTypeNeeded,
            LocalDate dateLimit,
            String bloodCenterName,
            Urgency urgency,
            Double distanceInKm,
            int goalBloodBags,
            int fulfilledBloodBags,
            boolean goalReached) {
    }
}
