package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.DonationSchema;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DonationMongoRepository extends MongoRepository<DonationSchema, String> {

  List<DonationSchema> findByDonorPersonId(String donorPersonId);

  List<DonationSchema> findByCompleted(boolean completed);

  long countByDonorPersonId(String donorPersonId);
}
