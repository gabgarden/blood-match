package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.repositories.RequesterRepositoryInterface;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.RequesterMongoRepository;
import bloodmatch.infra.persistence.schema.RequesterSchema;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RequesterRepositoryImpl implements RequesterRepositoryInterface {

  private final RequesterMongoRepository mongoRepository;
  private final PartyRepositoryInterface partyRepository;

  public RequesterRepositoryImpl(
      RequesterMongoRepository mongoRepository,
      PartyRepositoryInterface partyRepository) {
    this.mongoRepository = mongoRepository;
    this.partyRepository = partyRepository;
  }

  @Override
  public Optional<Requester> findByPartyId(DomainID partyId) {
    if (partyId == null)
      throw new IllegalArgumentException("Party id cannot be null");

    return mongoRepository.findByPartyId(partyId.getValue().toString())
        .map(schema -> schema.toDomain(partyRepository));
  }

  @Override
  public void save(Requester requester) {
    if (requester == null)
      throw new IllegalArgumentException("Requester cannot be null");

    mongoRepository.save(new RequesterSchema(requester));
  }
}
