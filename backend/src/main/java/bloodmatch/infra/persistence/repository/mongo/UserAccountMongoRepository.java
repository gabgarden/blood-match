package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.UserAccountSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountMongoRepository extends MongoRepository<UserAccountSchema, String> {

  Optional<UserAccountSchema> findByEmail(String email);

  Optional<UserAccountSchema> findByPartyId(String partyId);
}
