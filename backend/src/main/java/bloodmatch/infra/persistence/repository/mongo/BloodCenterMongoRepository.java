package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.BloodCenterSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodCenterMongoRepository extends MongoRepository<BloodCenterSchema, String> {

  Optional<BloodCenterSchema> findByOrganizationId(String organizationId);

  List<BloodCenterSchema> findByOrganizationIdIn(Collection<String> organizationIds);
}
