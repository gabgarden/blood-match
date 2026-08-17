package bloodmatch.infra.persistence.repository.mongo;

import bloodmatch.infra.persistence.schema.DonationSchema;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface DonationMongoRepository extends MongoRepository<DonationSchema, String> {

  List<DonationSchema> findByDonorPersonId(String donorPersonId);

  List<DonationSchema> findByCompleted(boolean completed);

  List<DonationSchema> findByCompletedTrueAndOrganizationIdInOrderByDonationDateAsc(
      List<String> organizationIds);

  List<DonationSchema> findByCompletedTrueAndOrganizationIdAndDonationDateBetweenOrderByDonationDateAsc(
      String organizationId,
      LocalDate startDate,
      LocalDate endDate);

  List<DonationSchema> findByPendingTrueAndOrganizationId(String organizationId);

  List<DonationSchema> findByPendingTrueAndOrganizationIdAndDonationDate(
      String organizationId,
      LocalDate donationDate);

  List<DonationSchema> findByPendingTrueAndOrganizationIdAndDonationDateBetween(
      String organizationId,
      LocalDate from,
      LocalDate to);

  long countByDonorPersonId(String donorPersonId);

  long countByDonorPersonIdAndCompletedTrue(String donorPersonId);
}
