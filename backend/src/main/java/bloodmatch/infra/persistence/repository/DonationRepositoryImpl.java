package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.mapping.PersistenceGraphLoader;
import bloodmatch.infra.persistence.repository.mongo.DonationMongoRepository;
import bloodmatch.infra.persistence.schema.DonationSchema;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class DonationRepositoryImpl implements DonationRepositoryInterface {

  private final DonationMongoRepository mongoRepository;
  private final PersistenceGraphLoader graphLoader;

  public DonationRepositoryImpl(
      DonationMongoRepository mongoRepository,
      PersistenceGraphLoader graphLoader) {
    this.mongoRepository = mongoRepository;
    this.graphLoader = graphLoader;
  }

  @Override
  public void save(Donation donation) {
    if (donation == null)
      throw new IllegalArgumentException("Donation cannot be null");

    OptimisticConcurrency.save(
        () -> mongoRepository.save(new DonationSchema(donation)),
        "Donation");
  }

  @Override
  public Optional<Donation> findById(DomainID id) {
    if (id == null)
      throw new IllegalArgumentException("Donation id cannot be null");

    return mongoRepository.findById(id.getValue().toString())
        .map(schema -> toDomainList(List.of(schema)).get(0));
  }

  @Override
  public List<Donation> findByDonorId(DomainID donorId) {
    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");

    return toDomainList(mongoRepository.findByDonorPersonId(donorId.getValue().toString()));
  }

  @Override
  public long countByDonorId(DomainID donorId) {
    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");

    return mongoRepository.countByDonorPersonIdAndCompletedTrue(donorId.getValue().toString());
  }

  @Override
  public List<Donation> findCompletedDonationsOrderedByDonationDateAsc() {
    return toDomainList(mongoRepository.findByCompleted(true)).stream()
        .sorted(java.util.Comparator.comparing(
            Donation::getDonationDate,
            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
        .toList();
  }

  @Override
  public List<Donation> findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
      List<DomainID> organizationIds) {
    if (organizationIds == null)
      throw new IllegalArgumentException("Organization ids cannot be null");

    if (organizationIds.isEmpty())
      return List.of();

    return toDomainList(
        mongoRepository.findCompletedByOrganizationIdInOrderByDonationDateAsc(
            toStringIds(organizationIds)));
  }

  @Override
  public List<Donation> findCompletedDonationsByOrganizationIdAndDateRange(
      DomainID organizationId,
      LocalDate startDate,
      LocalDate endDate) {
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");
    if (startDate == null)
      throw new IllegalArgumentException("Start date cannot be null");
    if (endDate == null)
      throw new IllegalArgumentException("End date cannot be null");

    return toDomainList(
        mongoRepository.findCompletedByOrganizationIdAndDonationDateInclusive(
            organizationId.getValue().toString(), startDate, endDate));
  }

  @Override
  public List<Donation> findCompletedDonationsByOrganizationIdsAndDateRange(
      List<DomainID> organizationIds,
      LocalDate startDate,
      LocalDate endDate) {
    if (organizationIds == null)
      throw new IllegalArgumentException("Organization ids cannot be null");
    if (startDate == null)
      throw new IllegalArgumentException("Start date cannot be null");
    if (endDate == null)
      throw new IllegalArgumentException("End date cannot be null");
    if (organizationIds.isEmpty())
      return List.of();

    return toDomainList(
        mongoRepository.findCompletedByOrganizationIdsAndDonationDateInclusive(
            toStringIds(organizationIds), startDate, endDate));
  }

  @Override
  public List<Donation> findPendingByOrganizationId(DomainID organizationId) {
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");

    return toDomainList(
        mongoRepository.findByPendingTrueAndOrganizationId(organizationId.getValue().toString()));
  }

  @Override
  public List<Donation> findPendingByOrganizationIdAndDate(DomainID organizationId, LocalDate date) {
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");
    if (date == null)
      throw new IllegalArgumentException("Date cannot be null");

    return toDomainList(
        mongoRepository.findPendingByOrganizationIdAndIntendedDate(
            organizationId.getValue().toString(), date));
  }

  @Override
  public List<Donation> findPendingByOrganizationIdAndDateRange(
      DomainID organizationId,
      LocalDate from,
      LocalDate to) {
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");
    if (from == null)
      throw new IllegalArgumentException("from cannot be null");
    if (to == null)
      throw new IllegalArgumentException("to cannot be null");

    return toDomainList(
        mongoRepository.findPendingByOrganizationIdAndIntendedDateInclusive(
            organizationId.getValue().toString(), from, to));
  }

  private List<Donation> toDomainList(List<DonationSchema> schemas) {
    return graphLoader.toDonations(schemas);
  }

  private static List<String> toStringIds(List<DomainID> organizationIds) {
    return organizationIds.stream()
        .map(DomainID::getValue)
        .map(Object::toString)
        .distinct()
        .toList();
  }
}
