package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class GetDonationRequestsByPartyIdUseCase {

    private final DonationRequestRepositoryInterface donationRequestRepository;

    public GetDonationRequestsByPartyIdUseCase(
                        DonationRequestRepositoryInterface donationRequestRepository) {

        this.donationRequestRepository = donationRequestRepository;
    }

    public List<OutputItem> execute(DomainID partyId) {
        return execute(partyId, LocalDate.now());
    }

    public List<OutputItem> execute(DomainID partyId, LocalDate currentDate) {

        if (partyId == null)
            throw new IllegalArgumentException("Party id cannot be null");
        if (currentDate == null)
            throw new IllegalArgumentException("Current date cannot be null");

        List<DonationRequest> userRequests =
                donationRequestRepository.findByRequesterPartyId(partyId);

        return userRequests.stream()
                .sorted(
                        Comparator.comparing(DonationRequest::getDateRequested)
                                .reversed()
                                .thenComparing(request -> request.getId().getValue()))
                .map(request -> toOutput(request, currentDate))
                .toList();
    }

    private OutputItem toOutput(
            DonationRequest request,
            LocalDate currentDate) {

        int fulfilledBloodBags = request.getFulfilledBloodBags();
        boolean goalReached = request.isGoalReached();

        return new OutputItem(
                request.getId().getValue().toString(),
                request.getBloodTypeNeeded().getType(),
                request.getDateRequested(),
                request.getDateLimit(),
                request.isActive(),
                request.isExpired(currentDate),
                request.getBloodCenter().getOrganization().getName(),
                request.getBloodCenter().getOrganization().getPhoneNumber().getValue(),
                request.getUrgency(),
                request.getGoalBloodBags(),
                fulfilledBloodBags,
                Math.max(0, request.getGoalBloodBags() - fulfilledBloodBags),
                goalReached);
    }

    public record OutputItem(
            String requestId,
            String bloodTypeNeeded,
            LocalDate dateRequested,
            LocalDate dateLimit,
            boolean active,
            boolean expired,
            String bloodCenterName,
            String bloodCenterPhoneNumber,
            Urgency urgency,
            int goalBloodBags,
            int fulfilledBloodBags,
            int remainingBloodBags,
            boolean goalReached) {
    }
}
