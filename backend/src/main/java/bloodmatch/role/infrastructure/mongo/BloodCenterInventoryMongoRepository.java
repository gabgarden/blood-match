package bloodmatch.role.infrastructure.mongo;

import bloodmatch.role.infrastructure.schema.BloodCenterInventorySchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BloodCenterInventoryMongoRepository
    extends MongoRepository<BloodCenterInventorySchema, String> {
}
