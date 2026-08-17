package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.DonationMongoRepository;
import bloodmatch.infra.persistence.schema.DonationSchema;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class DonationRepositoryImpl implements DonationRepositoryInterface {

  private final DonationMongoRepository mongoRepository;
  private final DonorRepositoryInterface donorRepository;
  private final BloodCenterRepositoryInterface bloodCenterRepository;

  public DonationRepositoryImpl(
      DonationMongoRepository mongoRepository,
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository) {
    this.mongoRepository = mongoRepository;
    this.donorRepository = donorRepository;
    this.bloodCenterRepository = bloodCenterRepository;
  }

  @Override
  public void save(Donation donation) {
    if (donation == null)
      throw new IllegalArgumentException("Donation cannot be null");

    mongoRepository.save(new DonationSchema(donation));
  }

  @Override
  public Optional<Donation> findById(DomainID id) {
    if (id == null)
      throw new IllegalArgumentException("Donation id cannot be null");

    return mongoRepository.findById(id.getValue().toString())
        .map(this::toDomain);
  }

  @Override
  public List<Donation> findByDonorId(DomainID donorId) {
    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");

    return mongoRepository.findByDonorPersonId(donorId.getValue().toString())
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public long countByDonorId(DomainID donorId) {
    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");

    return mongoRepository.countByDonorPersonIdAndCompletedTrue(donorId.getValue().toString());
  }

  @Override
  public List<Donation> findCompletedDonationsOrderedByDonationDateAsc() {
    return mongoRepository.findByCompleted(true)
        .stream()
        .map(this::toDomain)
      .sorted(java.util.Comparator.comparing(Donation::getDonationDate, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
        .toList();
  }

  @Override
  public List<Donation> findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
      List<DomainID> organizationIds) {
    if (organizationIds == null)
      throw new IllegalArgumentException("Organization ids cannot be null");

    if (organizationIds.isEmpty())
      return List.of();

    List<String> ids = organizationIds.stream()
        .map(DomainID::getValue)
        .map(Object::toString)
        .distinct()
        .toList();

    return mongoRepository.findByCompletedTrueAndOrganizationIdInOrderByDonationDateAsc(ids)
        .stream()
        .map(this::toDomain)
        .toList();
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

    return mongoRepository
        .findByCompletedTrueAndOrganizationIdAndDonationDateBetweenOrderByDonationDateAsc(
            organizationId.getValue().toString(), startDate, endDate)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Donation> findPendingByOrganizationId(DomainID organizationId) {
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");

    return mongoRepository.findByPendingTrueAndOrganizationId(organizationId.getValue().toString())
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<Donation> findPendingByOrganizationIdAndDate(DomainID organizationId, LocalDate date) {
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");
    if (date == null)
      throw new IllegalArgumentException("Date cannot be null");

    return mongoRepository
        .findByPendingTrueAndOrganizationIdAndDonationDate(organizationId.getValue().toString(), date)
        .stream()
        .map(this::toDomain)
        .toList();
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

    return mongoRepository
        .findByPendingTrueAndOrganizationIdAndDonationDateBetween(
            organizationId.getValue().toString(), from, to)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  private Donation toDomain(DonationSchema schema) {
    return schema.toDomain(donorRepository, bloodCenterRepository);
  }
}
