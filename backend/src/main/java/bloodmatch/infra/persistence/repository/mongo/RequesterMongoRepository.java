package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.RequesterSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequesterMongoRepository extends MongoRepository<RequesterSchema, String> {

  Optional<RequesterSchema> findByPartyId(String partyId);

  List<RequesterSchema> findByPartyIdIn(Collection<String> partyIds);
}
