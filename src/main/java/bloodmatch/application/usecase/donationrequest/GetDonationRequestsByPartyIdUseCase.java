package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.services.records.DonationRequestFulfillmentStatusRecord;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class GetDonationRequestsByPartyIdUseCase {

    private final DonationRequestRepositoryInterface donationRequestRepository;
    private final DonationRepositoryInterface donationRepository;
    private final DonationRequestFulfillmentService fulfillmentService;

    public GetDonationRequestsByPartyIdUseCase(
            DonationRequestRepositoryInterface donationRequestRepository,
            DonationRepositoryInterface donationRepository,
            DonationRequestFulfillmentService fulfillmentService) {

        this.donationRequestRepository = donationRequestRepository;
        this.donationRepository = donationRepository;
        this.fulfillmentService = fulfillmentService;
    }

    public List<OutputItem> execute(DomainID partyId) {

        if (partyId == null)
            throw new IllegalArgumentException("Party id cannot be null");

        LocalDate currentDate = LocalDate.now();

        List<DonationRequest> userRequests =
                donationRequestRepository.findByRequesterPartyId(partyId);

        List<DonationRequest> activeRequests =
                donationRequestRepository.findActiveRequests();

        List<Donation> donations =
                donationRepository.findCompletedDonationsOrderedByDonationDateAsc();

        Map<DomainID, DonationRequestFulfillmentStatusRecord> fulfillment =
                fulfillmentService.calculate(
                        activeRequests,
                        donations,
                        currentDate);

        return userRequests.stream()
                .sorted(
                        Comparator.comparing(DonationRequest::getDateRequested)
                                .reversed())
                .map(request -> toOutput(
                        request,
                        fulfillment.get(request.getId())))
                .toList();
    }

    private OutputItem toOutput(
            DonationRequest request,
            DonationRequestFulfillmentStatusRecord fulfillment) {

        return new OutputItem(
                request.getId().getValue().toString(),
                request.getBloodTypeNeeded().getType(),
                request.getDateRequested(),
                request.getDateLimit(),
                request.isActive(),
                request.getBloodCenter().getOrganization().getName(),
                request.getUrgency(),
                request.getGoalBloodBags(),
                fulfillment.fulfilledBloodBags(),
                fulfillment.goalReached());
    }

    public record OutputItem(
            String requestId,
            String bloodTypeNeeded,
            LocalDate dateRequested,
            LocalDate dateLimit,
            boolean active,
            String bloodCenterName,
            Urgency urgency,
            int goalBloodBags,
            int fulfilledBloodBags,
            boolean goalReached) {
    }
}