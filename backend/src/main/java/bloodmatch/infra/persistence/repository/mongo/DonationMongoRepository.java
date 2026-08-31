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

  @Query(value = "{ 'completed': true, 'organizationId': { $in: ?0 } }", sort = "{ 'donationDate': 1 }")
  List<DonationSchema> findCompletedByOrganizationIdInOrderByDonationDateAsc(
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

  @Query("{ 'pending': true, 'organizationId': ?0, $or: [ { 'intendedDate': ?1 }, { 'intendedDate': null, 'donationDate': ?1 } ] }")
  List<DonationSchema> findPendingByOrganizationIdAndIntendedDate(
      String organizationId,
      LocalDate intendedDate);

  @Query("{ 'pending': true, 'organizationId': ?0, $or: [ { 'intendedDate': { $gte: ?1, $lte: ?2 } }, { 'intendedDate': null, 'donationDate': { $gte: ?1, $lte: ?2 } } ] }")
  List<DonationSchema> findPendingByOrganizationIdAndIntendedDateInclusive(
      String organizationId,
      LocalDate from,
      LocalDate to);

  long countByDonorPersonId(String donorPersonId);

  long countByDonorPersonIdAndCompletedTrue(String donorPersonId);
}
