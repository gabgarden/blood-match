package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.party.Party;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.PartyMongoRepository;
import bloodmatch.infra.persistence.schema.PartySchema;
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

    mongoRepository.save(new PartySchema(party));
  }
}
