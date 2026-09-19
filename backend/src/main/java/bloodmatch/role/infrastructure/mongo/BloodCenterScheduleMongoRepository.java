package bloodmatch.role.infrastructure.mongo;

import bloodmatch.role.infrastructure.schema.BloodCenterScheduleSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BloodCenterScheduleMongoRepository
    extends MongoRepository<BloodCenterScheduleSchema, String> {
}
