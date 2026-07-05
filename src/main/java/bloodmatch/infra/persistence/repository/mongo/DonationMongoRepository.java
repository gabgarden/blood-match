package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.DonationSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationMongoRepository extends MongoRepository<DonationSchema, String> {

  List<DonationSchema> findByDonorPersonId(String donorPersonId);

  boolean existsByDonorPersonIdAndRequestId(String donorPersonId, String requestId);

  long countByDonorPersonId(String donorPersonId);
}
