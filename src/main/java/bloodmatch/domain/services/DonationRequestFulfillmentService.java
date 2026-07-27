package bloodmatch.domain.services;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.services.records.DonationRequestFulfillmentStatusRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;


@Service
public class DonationRequestFulfillmentService {

    public Map<DomainID, DonationRequestFulfillmentStatusRecord> calculate(
            List<DonationRequest> requests,
            List<Donation> donations,
            LocalDate currentDate) {

        requests = new ArrayList<>(requests);
        requests.sort(
                Comparator.comparing(DonationRequest::getDateRequested)
                        .thenComparing(request -> request.getId().getValue()));

        donations = new ArrayList<>(donations);
        donations.sort(
                Comparator.comparing(Donation::getDonationDate)
                        .thenComparing(donation -> donation.getId().getValue()));

        Map<DomainID, Integer> fulfilled = initialize(requests);
        Map<DomainID, List<DonationRequest>> requestsByBloodCenter = requests.stream()
                .collect(Collectors.groupingBy(request -> request.getBloodCenter().getId()));

        distributeDonations(
                requestsByBloodCenter,
                donations,
                currentDate,
                fulfilled);

        return buildResult(
                requests,
                fulfilled);
    }

    private Map<DomainID, Integer> initialize(
            List<DonationRequest> requests) {

        Map<DomainID, Integer> fulfilled = new HashMap<>();

        for (DonationRequest request : requests) {
            fulfilled.put(request.getId(), 0);
        }

        return fulfilled;
    }

    private void distributeDonations(
            Map<DomainID, List<DonationRequest>> requestsByBloodCenter,
            List<Donation> donations,
            LocalDate currentDate,
            Map<DomainID, Integer> fulfilled) {

        for (Donation donation : donations) {

            List<DonationRequest> requestsAtBloodCenter = requestsByBloodCenter.getOrDefault(
                    donation.getBloodCenter().getId(),
                    List.of());

            for (DonationRequest request : requestsAtBloodCenter) {

                if (!request.acceptsDonation(donation, currentDate))
                    continue;

                int currentFulfilled =
                        fulfilled.get(request.getId());

                if (currentFulfilled >= request.getGoalBloodBags())
                    continue;

                fulfilled.put(
                        request.getId(),
                        currentFulfilled + 1);

                break;
            }
        }
    }

    private Map<DomainID, DonationRequestFulfillmentStatusRecord> buildResult(
            List<DonationRequest> requests,
            Map<DomainID, Integer> fulfilled) {

        Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
                new HashMap<>();

        for (DonationRequest request : requests) {

            int fulfilledBloodBags =
                    fulfilled.get(request.getId());

            result.put(
                    request.getId(),
                    new DonationRequestFulfillmentStatusRecord(
                            fulfilledBloodBags,
                            fulfilledBloodBags >= request.getGoalBloodBags()));
        }

        return result;
    }
}
