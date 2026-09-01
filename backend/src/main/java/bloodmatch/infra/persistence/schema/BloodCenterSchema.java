package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.PartyRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "blood_centers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BloodCenterSchema {

  @Id
  private String id;
  @Version
  private Long version;
  private String organizationId;

  public BloodCenterSchema(BloodCenter bloodCenter) {
    if (bloodCenter == null)
      throw new IllegalArgumentException("BloodCenter cannot be null");

    this.id = bloodCenter.getId().getValue().toString();
    this.version = bloodCenter.getVersion();
    this.organizationId = bloodCenter.getOrganization().getId().getValue().toString();
  }

  public BloodCenter toDomain(Organization organization) {
    if (organization == null)
      throw new IllegalArgumentException("Organization not found");
    DomainID bloodCenterId = new DomainID(UUID.fromString(this.id));
    return BloodCenter.reconstitute(organization, bloodCenterId, this.version);
  }

  public BloodCenter toDomain(PartyRepositoryInterface partyRepository) {
    DomainID organizationId = new DomainID(UUID.fromString(this.organizationId));
    Organization organization = partyRepository.findById(organizationId)
        .filter(Organization.class::isInstance)
        .map(Organization.class::cast)
        .orElseThrow(() -> new IllegalArgumentException("Organization not found"));
    return toDomain(organization);
  }
}
