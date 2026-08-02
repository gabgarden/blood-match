package bloodmatch.application.usecase.donation.createcompleted;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CreateCompletedDonationUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final DonationRepositoryInterface donationRepository;
  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final DonationRequestFulfillmentService fulfillmentService;

  public CreateCompletedDonationUseCase(
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository,
      DonationRepositoryInterface donationRepository,
      DonationRequestRepositoryInterface donationRequestRepository,
      DonationRequestFulfillmentService fulfillmentService) {
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");
    if (bloodCenterRepository == null)
      throw new IllegalArgumentException("BloodCenterRepository cannot be null");
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");
    if (donationRequestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    if (fulfillmentService == null)
      throw new IllegalArgumentException("DonationRequestFulfillmentService cannot be null");
    this.donorRepository = donorRepository;
    this.bloodCenterRepository = bloodCenterRepository;
    this.donationRepository = donationRepository;
    this.donationRequestRepository = donationRequestRepository;
    this.fulfillmentService = fulfillmentService;
  }

  public Donation execute(
      DomainID personId,
      DomainID organizationId,
      LocalDate donationDate) {

    if (personId == null)
      throw new IllegalArgumentException("Person id cannot be null");
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");
    if (donationDate == null)
      throw new IllegalArgumentException("Donation date cannot be null");

    Donor donor = donorRepository.findByPartyId(personId)
        .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));

    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new IllegalArgumentException("Blood center role not found"));

    Donation donation = Donation.registerExternalDonation(donor, donationDate, bloodCenter, LocalDate.now());
    donor.registerDonation(donationDate, LocalDate.now());
    donorRepository.save(donor);
    donationRepository.save(donation);
    refreshFulfillment(bloodCenter.getOrganization().getId(), LocalDate.now());

    return donation;
  }

  private void refreshFulfillment(DomainID bloodCenterId, LocalDate currentDate) {
    List<bloodmatch.domain.donationrequest.DonationRequest> requests =
        donationRequestRepository.findActiveRequestsByBloodCenterIds(
            List.of(bloodCenterId),
            currentDate);

    if (requests.isEmpty()) {
      return;
    }

    List<Donation> donations = donationRepository
        .findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(List.of(bloodCenterId));

    fulfillmentService.synchronize(requests, donations, currentDate);

    for (bloodmatch.domain.donationrequest.DonationRequest request : requests) {
      donationRequestRepository.save(request);
    }
  }
}