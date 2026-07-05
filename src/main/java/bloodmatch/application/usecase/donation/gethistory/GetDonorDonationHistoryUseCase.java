package bloodmatch.application.usecase.donation.gethistory;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class GetDonorDonationHistoryUseCase {

  private final DonationRepositoryInterface donationRepository;

  public GetDonorDonationHistoryUseCase(DonationRepositoryInterface donationRepository) {
    this.donationRepository = donationRepository;
  }

  public List<OutputItem> execute(DomainID donorId) {
    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");

    return donationRepository.findByDonorId(donorId)
        .stream()
        .sorted(Comparator.comparing(Donation::getDonationDate).reversed())
        .map(donation -> new OutputItem(
            donation.getId().getValue().toString(),
            donation.getDonationDate(),
            donation.getBloodCenter().getOrganization().getName()))
        .toList();
  }

  public record OutputItem(
      String donationId,
      java.time.LocalDate date,
      String location) {
  }
}
