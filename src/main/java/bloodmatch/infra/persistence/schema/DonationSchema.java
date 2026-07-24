package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.UUID;

@Document(collection = "donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonationSchema {

  @Id
  private String id;
  private String donorPersonId;
  private String bloodCenterId;
  private LocalDate donationDate;
  private boolean completed;
  private boolean pending;
  private boolean cancelled;

  public DonationSchema(Donation donation) {
    if (donation == null)
      throw new IllegalArgumentException("Donation cannot be null");

    this.id = donation.getId().getValue().toString();
    this.donorPersonId = donation.getDonor().getPerson().getId().getValue().toString();
    this.bloodCenterId = donation.getBloodCenter().getOrganization().getId().getValue().toString();
    this.donationDate = donation.getDonationDate();
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

    DomainID bloodCenterPartyId = new DomainID(UUID.fromString(this.bloodCenterId));
    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(bloodCenterPartyId)
        .orElseThrow(() -> new IllegalArgumentException("Blood center role not found"));

    return Donation.reconstitute(
      new DomainID(UUID.fromString(this.id)),
      donor,
      this.donationDate,
      bloodCenter,
      this.completed,
      this.pending,
      this.cancelled);
  }
}
