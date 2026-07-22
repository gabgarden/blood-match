package bloodmatch.application.usecase.donationrequest;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;

@Service
public class UpdateDonationRequestDateLimitUseCase {

    private final DonationRequestRepositoryInterface donationRequestRepository;

    public UpdateDonationRequestDateLimitUseCase(DonationRequestRepositoryInterface donationRequestRepository) {
        this.donationRequestRepository = donationRequestRepository;
    }

    public DonationRequest execute(DomainID donationRequestId, LocalDate newDateLimit) {
        if (donationRequestId == null)
            throw new IllegalArgumentException("DonationRequest id cannot be null");
        if (newDateLimit == null)
            throw new IllegalArgumentException("newDateLimit cannot be null");
        if (newDateLimit.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("newDateLimit cannot be in the past");

        DonationRequest donationRequest = donationRequestRepository.findById(donationRequestId)
                .orElseThrow(() -> new IllegalArgumentException("DonationRequest not found"));

        donationRequest.setDateLimit(newDateLimit);
        donationRequestRepository.save(donationRequest);
        return donationRequest;
    }

}
