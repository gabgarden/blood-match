package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.repositories.BloodCenterDirectoryEntry;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.BloodCenterMongoRepository;
import bloodmatch.infra.persistence.schema.BloodCenterSchema;
import bloodmatch.infra.persistence.schema.PartySchema;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class BloodCenterRepositoryImpl implements BloodCenterRepositoryInterface {

  private final BloodCenterMongoRepository mongoRepository;
  private final PartyRepositoryInterface partyRepository;
  private final MongoTemplate mongoTemplate;

  public BloodCenterRepositoryImpl(
      BloodCenterMongoRepository mongoRepository,
      PartyRepositoryInterface partyRepository,
      MongoTemplate mongoTemplate) {
    this.mongoRepository = mongoRepository;
    this.partyRepository = partyRepository;
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Optional<BloodCenter> findByPartyId(DomainID partyId) {
    if (partyId == null)
      throw new IllegalArgumentException("Party id cannot be null");

    return mongoRepository.findByOrganizationId(partyId.getValue().toString())
        .map(schema -> schema.toDomain(partyRepository));
  }

  @Override
  public void save(BloodCenter bloodCenter) {
    if (bloodCenter == null)
      throw new IllegalArgumentException("BloodCenter cannot be null");

    mongoRepository.save(new BloodCenterSchema(bloodCenter));
  }

  @Override
  public List<BloodCenterDirectoryEntry> searchByOrganizationName(String query, int limit) {
    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("query cannot be blank");
    }
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }

    Pattern namePattern = Pattern.compile(Pattern.quote(query.trim()), Pattern.CASE_INSENSITIVE);

    Aggregation aggregation = Aggregation.newAggregation(
        Aggregation.lookup("parties", "organizationId", "_id", "organization"),
        Aggregation.unwind("organization"),
        Aggregation.match(
            Criteria.where("organization.partyType").is(PartySchema.TYPE_ORGANIZATION)
                .and("organization.name").regex(namePattern)),
        Aggregation.limit(limit),
        Aggregation.project()
            .andExclude("_id")
            .and("organizationId").as("organizationId")
            .and("organization.name").as("name")
            .and("organization.city").as("city")
            .and("organization.state").as("state"));

    AggregationResults<BloodCenterDirectoryEntry> results = mongoTemplate.aggregate(
        aggregation,
        "blood_centers",
        BloodCenterDirectoryEntry.class);

    return results.getMappedResults();
  }
}
