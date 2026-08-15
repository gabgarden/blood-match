package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.BloodCenterInventorySchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BloodCenterInventoryMongoRepository
    extends MongoRepository<BloodCenterInventorySchema, String> {
}
