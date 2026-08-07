package bloodmatch.application.usecase.donation.gethistory;

import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class GetDonorDonationHistoryUseCase {

  private final DonationRepositoryInterface donationRepository;

  public GetDonorDonationHistoryUseCase(DonationRepositoryInterface donationRepository) {
    this.donationRepository = donationRepository;
  }

  public List<OutputItem> execute(Input input) {
    if (input == null) {
      throw new ValidationException("Input cannot be null");
    }

    DomainID personId = DomainIdParser.parse(input.personId(), "personId");

    return donationRepository.findByDonorId(personId)
        .stream()
        .sorted(Comparator.comparing(Donation::getDonationDate).reversed())
        .map(OutputItem::from)
        .toList();
  }

  public record Input(String personId) {
  }

  public record OutputItem(
      String donationId,
      LocalDate date,
      String location) {

    public static OutputItem from(Donation donation) {
      return new OutputItem(
          donation.getId().getValue().toString(),
          donation.getDonationDate(),
          donation.getBloodCenter().getOrganization().getName());
    }
  }
}
