package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.party.Person;
import bloodmatch.domain.party.PersonRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.UUID;

@Document(collection = "donors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonorSchema {

  @Id
  private String id;
  @Version
  private Long version;
  private String personId;
  private String bloodType;
  private Double weight;
  private LocalDate weightUpdatedAt;
  private LocalDate lastDonationDate;
  private Double maxRecommendationDistanceKm;

  @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
  private double[] location;

  public DonorSchema(Donor donor) {
    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");

    this.id = donor.getId().getValue().toString();
    this.version = donor.getVersion();
    this.personId = donor.getPerson().getId().getValue().toString();
    this.bloodType = donor.getBloodType().getType();
    this.weight = donor.getWeight();
    this.weightUpdatedAt = donor.getWeightUpdatedAt();
    this.lastDonationDate = donor.getLastDonationDate();
    this.maxRecommendationDistanceKm = donor.getMaxRecommendationDistanceKm();

    Address address = donor.getPerson().getAddress();
    if (address != null && address.hasCoordinates()) {
        this.location = new double[]{address.getLongitude(), address.getLatitude()};
    }
  }

  public Donor toDomain(Person person) {
    if (person == null)
      throw new IllegalArgumentException("Person not found");
    DomainID donorId = new DomainID(UUID.fromString(this.id));
    return Donor.reconstitute(person, BloodType.of(bloodType), weight, lastDonationDate,
        maxRecommendationDistanceKm, donorId, weightUpdatedAt, this.version);
  }

  public Donor toDomain(PersonRepositoryInterface personRepository) {
    DomainID personId = new DomainID(UUID.fromString(this.personId));
    Person person = personRepository.findById(personId)
        .orElseThrow(() -> new IllegalArgumentException("Person not found"));
    return toDomain(person);
  }
}
