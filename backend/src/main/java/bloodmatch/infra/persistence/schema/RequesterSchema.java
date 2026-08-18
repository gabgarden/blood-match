package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.party.Party;
import bloodmatch.domain.party.PartyRepositoryInterface;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "requesters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequesterSchema {

  @Id
  private String id;
  private String partyId;

  public RequesterSchema(Requester requester) {
    if (requester == null)
      throw new IllegalArgumentException("Requester cannot be null");

    this.id = requester.getId().getValue().toString();
    this.partyId = requester.getParty().getId().getValue().toString();
  }

  public Requester toDomain(Party party) {
    if (party == null)
      throw new IllegalArgumentException("Party not found");
    DomainID requesterId = new DomainID(UUID.fromString(this.id));
    return Requester.reconstitute(party, requesterId);
  }

  public Requester toDomain(PartyRepositoryInterface partyRepository) {
    DomainID partyId = new DomainID(UUID.fromString(this.partyId));
    Party party = partyRepository.findById(partyId)
        .orElseThrow(() -> new IllegalArgumentException("Party not found"));
    return toDomain(party);
  }
}
