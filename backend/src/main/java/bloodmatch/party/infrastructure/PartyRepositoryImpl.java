package bloodmatch.party.infrastructure;
import bloodmatch.shared.infrastructure.persistence.repository.OptimisticConcurrency;

import bloodmatch.party.domain.Party;
import bloodmatch.party.domain.PartyRepositoryInterface;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.party.infrastructure.mongo.PartyMongoRepository;
import bloodmatch.party.infrastructure.schema.PartySchema;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PartyRepositoryImpl implements PartyRepositoryInterface {

  private final PartyMongoRepository mongoRepository;

  public PartyRepositoryImpl(PartyMongoRepository mongoRepository) {
    this.mongoRepository = mongoRepository;
  }

  @Override
  public Optional<Party> findById(DomainID partyId) {
    if (partyId == null)
      throw new IllegalArgumentException("Party id cannot be null");

    return mongoRepository.findById(partyId.getValue().toString())
        .map(PartySchema::toDomain);
  }

  @Override
  public void save(Party party) {
    if (party == null)
      throw new IllegalArgumentException("Party cannot be null");

    OptimisticConcurrency.save(
        () -> mongoRepository.save(new PartySchema(party)),
        "Party");
  }
}
