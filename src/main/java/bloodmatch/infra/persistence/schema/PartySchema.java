package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Party;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.UUID;

@Document(collection = "parties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartySchema {

  public static final String TYPE_PERSON = "PERSON";
  public static final String TYPE_ORGANIZATION = "ORGANIZATION";

  @Id
  private String id;
  private String partyType;
  private String name;
  private String cpf;
  private LocalDate birthDate;
  private String cnpj;
  private String street;
  private String number;
  private String neighborhood;
  private String city;
  private String state;
  private String zipCode;
  private Double latitude;
  private Double longitude;

  public PartySchema(Party party) {
    if (party == null)
      throw new IllegalArgumentException("Party cannot be null");

    this.id = party.getId().getValue().toString();
    this.name = party.getName();

    if (party instanceof Person person) {
      this.partyType = TYPE_PERSON;
      this.cpf = person.getCpf().getValue();
      this.birthDate = person.getBirthDate();
      this.cnpj = null;
      if (person.getAddress() != null) {
        this.street = person.getAddress().getStreet();
        this.number = person.getAddress().getNumber();
        this.neighborhood = person.getAddress().getNeighborhood();
        this.city = person.getAddress().getCity();
        this.state = person.getAddress().getState();
        this.zipCode = person.getAddress().getZipCode();
        this.latitude = person.getAddress().getLatitude();
        this.longitude = person.getAddress().getLongitude();
      }
      return;
    }

    if (party instanceof Organization organization) {
      this.partyType = TYPE_ORGANIZATION;
      this.cnpj = organization.getCnpj().getValue();
      this.cpf = null;
      this.birthDate = null;
      if (organization.getAddress() != null) {
        this.street = organization.getAddress().getStreet();
        this.number = organization.getAddress().getNumber();
        this.neighborhood = organization.getAddress().getNeighborhood();
        this.city = organization.getAddress().getCity();
        this.state = organization.getAddress().getState();
        this.zipCode = organization.getAddress().getZipCode();
        this.latitude = organization.getAddress().getLatitude();
        this.longitude = organization.getAddress().getLongitude();
      }
      return;
    }

    throw new IllegalStateException("Unsupported party subtype: " + party.getClass().getName());
  }

  public Party toDomain() {
    DomainID partyId = new DomainID(UUID.fromString(this.id));

    if (TYPE_PERSON.equals(this.partyType)) {
      Person person = new PersistedPerson(partyId, this.name, new CPF(this.cpf), this.birthDate);
      if (street != null && city != null && state != null && number != null) {
        person.changeAddress(new Address(street, number, neighborhood, city, state, zipCode, latitude, longitude));
      }
      return person;
    }

    if (TYPE_ORGANIZATION.equals(this.partyType)) {
      Organization organization = new PersistedOrganization(partyId, this.name, new CNPJ(this.cnpj));
      if (street != null && city != null && state != null && number != null) {
        organization.changeAddress(new Address(street, number, neighborhood, city, state, zipCode, latitude, longitude));
      }
      return organization;
    }

    throw new IllegalStateException("Unsupported party type: " + this.partyType);
  }

  private static class PersistedPerson extends Person {

    private PersistedPerson(DomainID id, String name, CPF cpf, LocalDate birthDate) {
      super(name, cpf, birthDate);
      setId(id);
    }
  }

  private static class PersistedOrganization extends Organization {

    private PersistedOrganization(DomainID id, String name, CNPJ cnpj) {
      super(name, cnpj);
      setId(id);
    }
  }
}
