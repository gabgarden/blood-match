package bloodmatch.application.usecase.donationrequest;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
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

  public Output execute(Input input) {
    return execute(input, LocalDate.now());
  }

  public Output execute(Input input, LocalDate currentDate) {
    if (input == null) {
      throw new ValidationException("Request body cannot be null");
    }
    if (currentDate == null) {
      throw new ValidationException("Current date cannot be null");
    }

    DomainID partyId = DomainIdParser.parse(input.partyId(), "partyId");
    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    BloodType bloodTypeNeeded = parseBloodType(input.bloodTypeNeeded());
    Urgency urgency = parseUrgency(input.urgency());

    if (input.goalBloodBags() == null) {
      throw new ValidationException("goalBloodBags cannot be null");
    }
    if (input.goalBloodBags() <= 0) {
      throw new ValidationException("Goal blood bags must be greater than zero");
    }
    if (input.dateLimit() == null) {
      throw new ValidationException("Date limit cannot be null");
    }
    if (input.dateLimit().isBefore(currentDate)) {
      throw new ValidationException("Date limit cannot be before current date");
    }

    Requester requester = requesterRepository.findByPartyId(partyId)
        .orElseThrow(() -> new NotFoundException("Requester role not found"));

    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    Organization organization = bloodCenter.getOrganization();
    if (organization.getAddress() != null && !organization.getAddress().hasCoordinates()) {
      organization.changeAddress(geocodingService.getCoordinatesFromAddress(organization.getAddress()));
      partyRepository.save(organization);
    }

    DonationRequest request = DonationRequest.create(
        requester,
        bloodCenter,
        bloodTypeNeeded,
        input.goalBloodBags(),
        input.dateLimit(),
        currentDate,
        urgency,
        input.directedTo());

    donationRequestRepository.save(request);
    return Output.from(request);
  }

  private static BloodType parseBloodType(String value) {
    if (value == null || value.isBlank()) {
      throw new ValidationException("bloodTypeNeeded cannot be blank");
    }
    try {
      return BloodType.of(value);
    } catch (IllegalArgumentException e) {
      throw new ValidationException(e.getMessage());
    }
  }

  private static Urgency parseUrgency(String value) {
    if (value == null || value.isBlank()) {
      throw new ValidationException("urgency cannot be blank");
    }
    try {
      return Urgency.valueOf(value.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ValidationException("Invalid urgency");
    }
  }

  public record Input(
      String partyId,
      String organizationId,
      String bloodTypeNeeded,
      Integer goalBloodBags,
      LocalDate dateLimit,
      String urgency,
      String directedTo) {
  }

  public record Output(String id) {
    public static Output from(DonationRequest request) {
      return new Output(request.getId().getValue().toString());
    }
  }
}
