package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.PartySchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartyMongoRepository extends MongoRepository<PartySchema, String> {

  Optional<PartySchema> findByIdAndPartyType(String id, String partyType);
}
