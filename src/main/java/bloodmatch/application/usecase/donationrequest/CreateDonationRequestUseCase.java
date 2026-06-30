package bloodmatch.application.usecase.donationrequest;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
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
  private final PartyRepositoryInterface partyRepository;
  private final GeocodingServiceInterface geocodingService;

  public CreateDonationRequestUseCase(DonationRequestRepositoryInterface donationRequestRepository,
      RequesterRepositoryInterface requesterRepository,
      PartyRepositoryInterface partyRepository,
      GeocodingServiceInterface geocodingService) {
    if (donationRequestRepository == null) {
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    }
    if (requesterRepository == null) {
      throw new IllegalArgumentException("RequesterRepository cannot be null");
    }
    if (partyRepository == null) {
      throw new IllegalArgumentException("PartyRepository cannot be null");
    }
    if (geocodingService == null) {
      throw new IllegalArgumentException("GeocodingService cannot be null");
    }
    this.donationRequestRepository = donationRequestRepository;
    this.requesterRepository = requesterRepository;
    this.partyRepository = partyRepository;
    this.geocodingService = geocodingService;
  }

  public DonationRequest execute(
      DomainID requesterID,
      DomainID bloodCenterID,
      BloodType bloodTypeNeeded,
      LocalDate dateLimit,
      Urgency urgency) {
    return execute(
        requesterID,
        bloodCenterID,
        bloodTypeNeeded,
        dateLimit,
        LocalDate.now(),
        urgency);
  }

  public DonationRequest execute(
      DomainID requesterID,
      DomainID bloodCenterID,
      BloodType bloodTypeNeeded,
      LocalDate dateLimit,
      LocalDate currentDate,
      Urgency urgency) {

    if (requesterID == null)
      throw new IllegalArgumentException("Requester id cannot be null");
    if (bloodCenterID == null)
      throw new IllegalArgumentException("Blood center id cannot be null");
    if (bloodTypeNeeded == null)
      throw new IllegalArgumentException("Blood type needed cannot be null");
    if (dateLimit == null)
      throw new IllegalArgumentException("Date limit cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (urgency == null)
      throw new IllegalArgumentException("Urgency cannot be null");
    if (dateLimit.isBefore(currentDate))
      throw new IllegalArgumentException("Date limit cannot be before current date");

    Requester requester = requesterRepository.findByPartyId(requesterID)
      .orElseThrow(() -> new IllegalArgumentException("Requester role not found"));

    Organization organization = partyRepository.findById(bloodCenterID)
      .filter(Organization.class::isInstance)
      .map(Organization.class::cast)
      .orElseThrow(() -> new IllegalArgumentException("Blood center organization not found"));

    if (organization.getAddress() != null && !organization.getAddress().hasCoordinates()) {
      organization.changeAddress(geocodingService.getCoordinatesFromAddress(organization.getAddress()));
      partyRepository.save(organization);
    }

    BloodCenter bloodCenter = new BloodCenter(organization);

    DonationRequest request = DonationRequest.create(
        requester,
        bloodCenter,
        bloodTypeNeeded,
        dateLimit,
        currentDate,
        urgency);

    donationRequestRepository.save(request);
    return request;
  }
}