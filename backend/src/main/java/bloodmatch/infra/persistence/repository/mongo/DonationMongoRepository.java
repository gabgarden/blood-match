package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.DonationSchema;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DonationMongoRepository extends MongoRepository<DonationSchema, String> {

  List<DonationSchema> findByDonorPersonId(String donorPersonId);

  List<DonationSchema> findByCompleted(boolean completed);

  List<DonationSchema> findByCompletedTrueAndOrganizationIdInOrderByDonationDateAsc(
      List<String> organizationIds);

  @Query("{ 'completed': true, 'organizationId': ?0, 'donationDate': { $gte: ?1, $lte: ?2 } }")
  List<DonationSchema> findCompletedByOrganizationIdAndDonationDateInclusive(
      String organizationId,
      LocalDate startDate,
      LocalDate endDate);

  @Query("{ 'completed': true, 'organizationId': { $in: ?0 }, 'donationDate': { $gte: ?1, $lte: ?2 } }")
  List<DonationSchema> findCompletedByOrganizationIdsAndDonationDateInclusive(
      Collection<String> organizationIds,
      LocalDate startDate,
      LocalDate endDate);

  List<DonationSchema> findByPendingTrueAndOrganizationId(String organizationId);

  List<DonationSchema> findByPendingTrueAndOrganizationIdAndDonationDate(
      String organizationId,
      LocalDate donationDate);

  @Query("{ 'pending': true, 'organizationId': ?0, 'donationDate': { $gte: ?1, $lte: ?2 } }")
  List<DonationSchema> findPendingByOrganizationIdAndDonationDateInclusive(
      String organizationId,
      LocalDate from,
      LocalDate to);

  long countByDonorPersonId(String donorPersonId);

  long countByDonorPersonIdAndCompletedTrue(String donorPersonId);
}
