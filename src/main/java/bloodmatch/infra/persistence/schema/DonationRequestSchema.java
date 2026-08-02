package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.RequesterRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.UUID;

@Document(collection = "donation_requests")
@CompoundIndex(name = "active_blood_type_date_limit", def = "{'active': 1, 'bloodTypeNeeded': 1, 'dateLimit': 1}")
@CompoundIndex(name = "active_blood_center_date_requested", def = "{'active': 1, 'bloodCenterId': 1, 'dateRequested': 1, '_id': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonationRequestSchema {

  @Id
  private String id;
  @Version
  private Long version;
  private String requesterId;
  private String bloodCenterId;
  private String bloodTypeNeeded;
  private int goalBloodBags;
  private int fulfilledBloodBags;
  private LocalDate dateRequested;
  private LocalDate dateLimit;
  private boolean active;
  private String urgency;
  private String directedTo;
  @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
  private double[] location;

  public DonationRequestSchema(DonationRequest donationRequest) {
    if (donationRequest == null)
      throw new IllegalArgumentException("DonationRequest cannot be null");

    this.id = donationRequest.getId().getValue().toString();
    this.version = donationRequest.getVersion();
    this.requesterId = donationRequest.getRequester().getParty().getId().getValue().toString();
    this.bloodCenterId = donationRequest.getBloodCenter().getOrganization().getId().getValue().toString();
    this.bloodTypeNeeded = donationRequest.getBloodTypeNeeded().getType();
    this.goalBloodBags = donationRequest.getGoalBloodBags();
    this.fulfilledBloodBags = donationRequest.getFulfilledBloodBags();
    this.dateRequested = donationRequest.getDateRequested();
    this.dateLimit = donationRequest.getDateLimit();
    this.active = donationRequest.isActive();
    this.urgency = donationRequest.getUrgency().name();
    this.directedTo = donationRequest.getDirectedTo();
    if (donationRequest.getBloodCenter().getOrganization().getAddress() != null
        && donationRequest.getBloodCenter().getOrganization().getAddress().hasCoordinates()) {
      this.location = new double[]{
          donationRequest.getBloodCenter().getOrganization().getAddress().getLongitude(),
          donationRequest.getBloodCenter().getOrganization().getAddress().getLatitude()};
    }
 
  }

  public DonationRequest toDomain(
      RequesterRepositoryInterface requesterRepository,
      BloodCenterRepositoryInterface bloodCenterRepository) {
    DomainID requesterId = new DomainID(UUID.fromString(this.requesterId));
    DomainID bloodCenterId = new DomainID(UUID.fromString(this.bloodCenterId));

    Requester requester = requesterRepository.findByPartyId(requesterId)
        .orElseThrow(() -> new IllegalArgumentException("Requester role not found"));

    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(bloodCenterId)
        .orElseThrow(() -> new IllegalArgumentException("Blood center role not found"));

    return DonationRequest.reconstitute(
        new DomainID(UUID.fromString(this.id)),
        requester,
        bloodCenter,
        BloodType.of(this.bloodTypeNeeded),
        this.goalBloodBags,
        this.dateRequested,
        this.dateLimit,
        this.active,
        Urgency.valueOf(this.urgency),
        this.directedTo,
        this.fulfilledBloodBags,
        this.version);
  }
}