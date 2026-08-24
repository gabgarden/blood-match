package bloodmatch.application.usecase.bloodcenter.appointments;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.shared.PartyOwnership;
import bloodmatch.application.shared.TimeFormats;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class GetBloodCenterAppointmentsUseCase {

  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final DonationRepositoryInterface donationRepository;

  public GetBloodCenterAppointmentsUseCase(
      BloodCenterRepositoryInterface bloodCenterRepository,
      DonationRepositoryInterface donationRepository) {
    this.bloodCenterRepository = bloodCenterRepository;
    this.donationRepository = donationRepository;
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

    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    PartyOwnership.requireSameParty(organizationId, input.actorPartyId());
    bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    LocalDate from = input.from() == null ? currentDate : input.from();
    LocalDate to = input.to() == null ? currentDate.plusDays(14) : input.to();
    if (from.isAfter(to)) {
      throw new ValidationException("from cannot be after to");
    }

    return donationRepository.findPendingByOrganizationIdAndDateRange(organizationId, from, to).stream()
        .filter(Donation::isPending)
        .sorted(Comparator
            .comparing(Donation::getIntendedDate)
            .thenComparing(Donation::getExpectedTime, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(OutputItem::from)
        .toList();
  }

  public record Input(String organizationId, LocalDate from, LocalDate to, String actorPartyId) {
  }

  public record OutputItem(
      String donationId,
      LocalDate expectedDate,
      String expectedTime,
      String status,
      String donorName,
      String donorBloodType,
      String donorPhone) {

    public static OutputItem from(Donation donation) {
      return new OutputItem(
          donation.getId().getValue().toString(),
          donation.getIntendedDate(),
          TimeFormats.format(donation.getExpectedTime()),
          "PENDING",
          donation.getDonor().getPerson().getName(),
          donation.getDonor().getBloodType().getType(),
          donation.getDonor().getPerson().getPhoneNumber().getValue());
    }
  }
}
