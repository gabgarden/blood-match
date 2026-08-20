package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Document(collection = "donations")
@CompoundIndexes({
    @CompoundIndex(name = "completed_organization_donation_date", def = "{'organizationId': 1, 'donationDate': 1}"),
    @CompoundIndex(name = "pending_organization_intended_date_time", def = "{'organizationId': 1, 'intendedDate': 1, 'expectedTime': 1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonationSchema {

  @Id
  private String id;
  private String donorPersonId;
  private String organizationId;
  private LocalDate intendedDate;
  private LocalDate donationDate;
  private LocalDate cancelledAt;
  private LocalTime expectedTime;
  private boolean completed;
  private boolean pending;
  private boolean cancelled;

  public DonationSchema(Donation donation) {
    if (donation == null)
      throw new IllegalArgumentException("Donation cannot be null");

    this.id = donation.getId().getValue().toString();
    this.donorPersonId = donation.getDonor().getPerson().getId().getValue().toString();
    this.organizationId = donation.getBloodCenter().getOrganization().getId().getValue().toString();
    this.intendedDate = donation.getIntendedDate();
    this.donationDate = donation.getDonationDate();
    this.cancelledAt = donation.getCancelledAt();
    this.expectedTime = donation.getExpectedTime();
    this.completed = donation.isCompleted();
    this.pending = donation.isPending();
    this.cancelled = donation.isCancelled();
  }

  public Donation toDomain(Donor donor, BloodCenter bloodCenter) {
    LocalDate intended = this.intendedDate;
    LocalDate actual = this.donationDate;
    LocalDate cancelledOn = this.cancelledAt;

    if (intended == null && cancelledOn == null) {
      if (this.pending) {
        intended = this.donationDate;
        actual = null;
      } else if (this.cancelled) {
        intended = this.donationDate;
        actual = null;
        cancelledOn = this.donationDate;
      }
    }

    return Donation.reconstitute(
      new DomainID(UUID.fromString(this.id)),
      donor,
      intended,
      actual,
      cancelledOn,
      bloodCenter,
      this.expectedTime);
  }

  public Donation toDomain(
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository) {

    DomainID donorId = new DomainID(UUID.fromString(this.donorPersonId));
    Donor donor = donorRepository.findByPartyId(donorId)
        .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));

    DomainID organizationId = new DomainID(UUID.fromString(this.organizationId));
    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new IllegalArgumentException("Blood center role not found"));

    return toDomain(donor, bloodCenter);
  }
}
