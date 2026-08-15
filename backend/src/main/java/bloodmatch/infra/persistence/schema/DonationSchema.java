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
    @CompoundIndex(name = "completed_organization_donation_date", def = "{'completed': 1, 'organizationId': 1, 'donationDate': 1}"),
    @CompoundIndex(name = "pending_organization_donation_date_time", def = "{'pending': 1, 'organizationId': 1, 'donationDate': 1, 'expectedTime': 1}")
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
  private LocalDate donationDate;
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
    this.donationDate = donation.getDonationDate();
    this.expectedTime = donation.getExpectedTime();
    this.completed = donation.isCompleted();
    this.pending = donation.isPending();
    this.cancelled = donation.isCancelled();
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

    return Donation.reconstitute(
      new DomainID(UUID.fromString(this.id)),
      donor,
      this.donationDate,
      bloodCenter,
      this.completed,
      this.pending,
      this.cancelled,
      this.expectedTime);
  }
}
