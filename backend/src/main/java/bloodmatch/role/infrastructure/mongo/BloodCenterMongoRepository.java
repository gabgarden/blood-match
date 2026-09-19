package bloodmatch.role.infrastructure.mongo;

import bloodmatch.role.infrastructure.schema.BloodCenterSchema;
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
