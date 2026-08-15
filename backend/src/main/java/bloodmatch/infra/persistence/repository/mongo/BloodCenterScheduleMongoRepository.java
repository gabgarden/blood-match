package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.BloodCenterScheduleSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BloodCenterScheduleMongoRepository
    extends MongoRepository<BloodCenterScheduleSchema, String> {
}
