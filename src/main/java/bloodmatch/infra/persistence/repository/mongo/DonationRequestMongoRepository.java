package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.DonationRequestSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDate;
import java.util.Collection;

@Repository
public interface DonationRequestMongoRepository extends MongoRepository<DonationRequestSchema, String> {

  List<DonationRequestSchema> findByActiveOrderByDateRequestedAscIdAsc(boolean active);

  List<DonationRequestSchema> findByRequesterId(String requesterId);

  List<DonationRequestSchema> findByActiveTrueAndDateLimitGreaterThanEqualAndBloodTypeNeededInAndLocationNear(
      LocalDate currentDate, Collection<String> bloodTypes, Point location, Distance distance);

  List<DonationRequestSchema> findByActiveTrueAndDateLimitGreaterThanEqualAndOrganizationIdInOrderByDateRequestedAscIdAsc(
      LocalDate currentDate, Collection<String> organizationIds);
}
