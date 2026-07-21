package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.repositories.RequesterRepositoryInterface;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.UUID;

@Document(collection = "donation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonationRequestSchema {

  @Id
  private String id;
  private String requesterId;
  private String bloodCenterId;
  private String bloodTypeNeeded;
  private int goalBloodBags;
  private LocalDate dateRequested;
  private LocalDate dateLimit;
  private boolean active;
  private String urgency;

  public DonationRequestSchema(DonationRequest donationRequest) {
    if (donationRequest == null)
      throw new IllegalArgumentException("DonationRequest cannot be null");

    this.id = donationRequest.getId().getValue().toString();
    this.requesterId = donationRequest.getRequester().getParty().getId().getValue().toString();
    this.bloodCenterId = donationRequest.getBloodCenter().getOrganization().getId().getValue().toString();
    this.bloodTypeNeeded = donationRequest.getBloodTypeNeeded().getType();
    this.goalBloodBags = donationRequest.getGoalBloodBags();
    this.dateRequested = donationRequest.getDateRequested();
    this.dateLimit = donationRequest.getDateLimit();
    this.active = donationRequest.isActive();
    this.urgency = donationRequest.getUrgency().name();
 
  }

  public DonationRequest toDomain(
      RequesterRepositoryInterface requesterRepository,
      PartyRepositoryInterface partyRepository) {
    DomainID requesterId = new DomainID(UUID.fromString(this.requesterId));
    DomainID bloodCenterId = new DomainID(UUID.fromString(this.bloodCenterId));

    Requester requester = requesterRepository.findByPartyId(requesterId)
        .orElseThrow(() -> new IllegalArgumentException("Requester role not found"));

    Organization organization = partyRepository.findById(bloodCenterId)
        .filter(Organization.class::isInstance)
        .map(Organization.class::cast)
        .orElseThrow(() -> new IllegalArgumentException("Blood center organization not found"));

    BloodCenter bloodCenter = new BloodCenter(organization);

    
  
    

    return DonationRequest.reconstitute(
        new DomainID(UUID.fromString(this.id)),
        requester,
        bloodCenter,
        BloodType.of(this.bloodTypeNeeded),
        this.goalBloodBags,
        this.dateRequested,
        this.dateLimit,
        this.active,
        Urgency.valueOf(this.urgency));
  }
}
