package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.BloodCenterMongoRepository;
import bloodmatch.infra.persistence.schema.BloodCenterSchema;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BloodCenterRepositoryImpl implements BloodCenterRepositoryInterface {

  private final BloodCenterMongoRepository mongoRepository;
  private final PartyRepositoryInterface partyRepository;

  public BloodCenterRepositoryImpl(
      BloodCenterMongoRepository mongoRepository,
      PartyRepositoryInterface partyRepository) {
    this.mongoRepository = mongoRepository;
    this.partyRepository = partyRepository;
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
}
