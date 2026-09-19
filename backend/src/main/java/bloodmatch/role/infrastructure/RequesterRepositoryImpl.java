package bloodmatch.role.infrastructure;
import bloodmatch.shared.infrastructure.persistence.repository.OptimisticConcurrency;

import bloodmatch.role.domain.requester.RequesterRepositoryInterface;
import bloodmatch.party.domain.PartyRepositoryInterface;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.role.infrastructure.mongo.RequesterMongoRepository;
import bloodmatch.role.infrastructure.schema.RequesterSchema;
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

    OptimisticConcurrency.save(
        () -> mongoRepository.save(new RequesterSchema(requester)),
        "Requester");
  }
}
