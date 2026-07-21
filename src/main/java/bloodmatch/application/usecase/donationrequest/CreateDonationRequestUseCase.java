package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.repositories.RequesterRepositoryInterface;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.services.GeocodingServiceInterface;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class CreateDonationRequestUseCase {

  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final RequesterRepositoryInterface requesterRepository;
  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final PartyRepositoryInterface partyRepository;
  private final GeocodingServiceInterface geocodingService;

  public CreateDonationRequestUseCase(DonationRequestRepositoryInterface donationRequestRepository,
      RequesterRepositoryInterface requesterRepository,
      BloodCenterRepositoryInterface bloodCenterRepository,
      PartyRepositoryInterface partyRepository,
      GeocodingServiceInterface geocodingService) {
    if (donationRequestRepository == null) {
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    }
    if (requesterRepository == null) {
      throw new IllegalArgumentException("RequesterRepository cannot be null");
    }
    if (bloodCenterRepository == null) {
      throw new IllegalArgumentException("BloodCenterRepository cannot be null");
    }
    if (partyRepository == null) {
      throw new IllegalArgumentException("PartyRepository cannot be null");
    }
    if (geocodingService == null) {
      throw new IllegalArgumentException("GeocodingService cannot be null");
    }
    this.donationRequestRepository = donationRequestRepository;
    this.requesterRepository = requesterRepository;
    this.bloodCenterRepository = bloodCenterRepository;
    this.partyRepository = partyRepository;
    this.geocodingService = geocodingService;
  }

  public DonationRequest execute(
      DomainID partyId,
      DomainID bloodCenterID,
      BloodType bloodTypeNeeded,
      int goalBloodBags,
      LocalDate dateLimit,
      Urgency urgency) {
    return execute(
        partyId,
        bloodCenterID,
        bloodTypeNeeded,
        goalBloodBags,
        dateLimit,
        LocalDate.now(),
        urgency);
  }

  public DonationRequest execute(
      DomainID partyId,
      DomainID bloodCenterID,
      BloodType bloodTypeNeeded,
      int goalBloodBags,
      LocalDate dateLimit,
      LocalDate currentDate,
      Urgency urgency) {

    if (partyId == null)
      throw new IllegalArgumentException("Party id cannot be null");
    if (bloodCenterID == null)
      throw new IllegalArgumentException("Blood center id cannot be null");
    if (bloodTypeNeeded == null)
      throw new IllegalArgumentException("Blood type needed cannot be null");
    if (goalBloodBags <= 0)
      throw new IllegalArgumentException("Goal blood bags must be greater than zero");
    if (dateLimit == null)
      throw new IllegalArgumentException("Date limit cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (urgency == null)
      throw new IllegalArgumentException("Urgency cannot be null");
    if (dateLimit.isBefore(currentDate))
      throw new IllegalArgumentException("Date limit cannot be before current date");

    Requester requester = requesterRepository.findByPartyId(partyId)
      .orElseThrow(() -> new IllegalArgumentException("Requester role not found"));

    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(bloodCenterID)
      .orElseThrow(() -> new IllegalArgumentException("Blood center role not found"));

    // Ensure blood center organization address has coordinates
    Organization organization = bloodCenter.getOrganization();
    if (organization.getAddress() != null && !organization.getAddress().hasCoordinates()) {
      organization.changeAddress(geocodingService.getCoordinatesFromAddress(organization.getAddress()));
      partyRepository.save(organization);
    }

    DonationRequest request = DonationRequest.create(
        requester,
        bloodCenter,
        bloodTypeNeeded,
        goalBloodBags,
        dateLimit,
        currentDate,
        urgency);

    donationRequestRepository.save(request);
    return request;
  }
}