package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Party;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
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
  @Version
  private Long version;
  private String partyType;
  private String name;
  private String phoneNumber;
  private String cpf;
  private LocalDate birthDate;
  private String cnpj;
  private String street;
  private String city;
  private String state;
  private String zipCode;
  private Double latitude;
  private Double longitude;

  public PartySchema(Party party) {
    if (party == null)
      throw new IllegalArgumentException("Party cannot be null");

    this.id = party.getId().getValue().toString();
    this.version = party.getVersion();
    this.name = party.getName();

    if (party instanceof Person person) {
      this.partyType = TYPE_PERSON;
      this.phoneNumber = person.getPhoneNumber().getValue();
      this.cpf = person.getCpf().getValue();
      this.birthDate = person.getBirthDate();
      this.cnpj = null;
      if (person.getAddress() != null) {
        this.street = person.getAddress().getStreet();
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
      this.phoneNumber = organization.getPhoneNumber().getValue();
      this.cnpj = organization.getCnpj().getValue();
      this.cpf = null;
      this.birthDate = null;
      if (organization.getAddress() != null) {
        this.street = organization.getAddress().getStreet();
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
      Person person = new PersistedPerson(partyId, this.name, new PhoneNumber(this.phoneNumber), new CPF(this.cpf), this.birthDate, this.version);
      if (street != null && city != null && state != null) {
        person.changeAddress(new Address(street, city, state, zipCode, latitude, longitude));
      }
      return person;
    }

    if (TYPE_ORGANIZATION.equals(this.partyType)) {
      Organization organization = new PersistedOrganization(partyId, this.name, new PhoneNumber(this.phoneNumber), new CNPJ(this.cnpj), this.version);
      if (street != null && city != null && state != null) {
        organization.changeAddress(new Address(street, city, state, zipCode, latitude, longitude));
      }
      return organization;
    }

    throw new IllegalStateException("Unsupported party type: " + this.partyType);
  }

  private static class PersistedPerson extends Person {

    private PersistedPerson(DomainID id, String name, PhoneNumber phonenumber, CPF cpf, LocalDate birthDate, Long version) {
      super(name, phonenumber, cpf, birthDate);
      setId(id);
      setVersion(version);
    }
  }

  private static class PersistedOrganization extends Organization {

    private PersistedOrganization(DomainID id, String name, PhoneNumber phonenumber, CNPJ cnpj, Long version) {
      super(name, phonenumber, cnpj);
      setId(id);
      setVersion(version);
    }
  }
}
