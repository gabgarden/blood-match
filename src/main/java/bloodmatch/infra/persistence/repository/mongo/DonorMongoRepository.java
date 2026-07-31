package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.DonorSchema;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonorMongoRepository extends MongoRepository<DonorSchema, String> {

  Optional<DonorSchema> findByPersonId(String personId);

  List<DonorSchema> findByLocationNear(Point location, Distance distance);

  List<DonorSchema> findAll();
}