package bloodmatch.application.usecase.donationrequest;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;

@Service
public class UpdateDonationRequestDateLimitUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;

  public UpdateDonationRequestDateLimitUseCase(DonationRequestRepositoryInterface donationRequestRepository) {
    this.donationRequestRepository = donationRequestRepository;
  }

  public Output execute(Input input) {
    if (input == null) {
      throw new ValidationException("Request body cannot be null");
    }

    DomainID donationRequestId = DomainIdParser.parse(input.requestId(), "requestId");
    if (input.newDateLimit() == null) {
      throw new ValidationException("newDateLimit cannot be null");
    }
    if (input.newDateLimit().isBefore(LocalDate.now())) {
      throw new ValidationException("newDateLimit cannot be in the past");
    }

    DonationRequest donationRequest = donationRequestRepository.findById(donationRequestId)
        .orElseThrow(() -> new NotFoundException("DonationRequest not found"));

    donationRequest.setDateLimit(input.newDateLimit());
    donationRequestRepository.save(donationRequest);
    return Output.from(donationRequest);
  }

  public record Input(String requestId, LocalDate newDateLimit) {
  }

  public record Output(String id, LocalDate dateLimit) {
    public static Output from(DonationRequest donationRequest) {
      return new Output(
          donationRequest.getId().getValue().toString(),
          donationRequest.getDateLimit());
    }
  }
}
