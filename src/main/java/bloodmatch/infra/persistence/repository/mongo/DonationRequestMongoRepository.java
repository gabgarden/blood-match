package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.DonationRequestSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRequestMongoRepository extends MongoRepository<DonationRequestSchema, String> {

  List<DonationRequestSchema> findByActiveOrderByDateRequestedAscIdAsc(boolean active);

  List<DonationRequestSchema> findByRequesterId(String requesterId);
}
