package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.roles.requester.RequesterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.infra.persistence.repository.mongo.DonationRequestMongoRepository;
import bloodmatch.infra.persistence.schema.DonationRequestSchema;
import org.springframework.stereotype.Repository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class DonationRequestRepositoryImpl implements DonationRequestRepositoryInterface {

  private final DonationRequestMongoRepository mongoRepository;
  private final RequesterRepositoryInterface requesterRepository;
  private final BloodCenterRepositoryInterface bloodCenterRepository;

  public DonationRequestRepositoryImpl(
      DonationRequestMongoRepository mongoRepository,
      RequesterRepositoryInterface requesterRepository,
      BloodCenterRepositoryInterface bloodCenterRepository) {
    this.mongoRepository = mongoRepository;
    this.requesterRepository = requesterRepository;
    this.bloodCenterRepository = bloodCenterRepository;
  }

  @Override
  public void save(DonationRequest request) {
    if (request == null)
      throw new IllegalArgumentException("DonationRequest cannot be null");

    DonationRequestSchema schema = new DonationRequestSchema(request);
    try {
      mongoRepository.save(schema);
    } catch (OptimisticLockingFailureException e) {
      throw new IllegalStateException("Donation request was changed by another operation. Reload it and try again.", e);
    }
  }

  @Override
  public Optional<DonationRequest> findById(DomainID id) {
    if (id == null)
      throw new IllegalArgumentException("Donation request id cannot be null");

    return mongoRepository.findById(id.getValue().toString())
        .map(this::toDomain);
  }

  @Override
  public List<DonationRequest> findActiveRequests() {
    return mongoRepository.findByActiveOrderByDateRequestedAscIdAsc(true)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<DonationRequest> findActiveRequestsForDonor(
      BloodType donorBloodType,
      Address donorAddress,
      double maxDistanceInKm,
      LocalDate currentDate) {
    if (donorBloodType == null)
      throw new IllegalArgumentException("Donor blood type cannot be null");
    if (donorAddress == null || !donorAddress.hasCoordinates())
      return List.of();
    if (maxDistanceInKm <= 0)
      throw new IllegalArgumentException("Maximum distance must be greater than zero");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    Point donorLocation = new Point(donorAddress.getLongitude(), donorAddress.getLatitude());
    Distance maxDistance = new Distance(maxDistanceInKm, Metrics.KILOMETERS);

    return mongoRepository
        .findByActiveTrueAndDateLimitGreaterThanEqualAndBloodTypeNeededInAndLocationNear(
            currentDate, donorBloodType.getCompatibleRecipientTypes(), donorLocation, maxDistance)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<DonationRequest> findActiveRequestsByOrganizationIds(
      List<DomainID> organizationIds,
      LocalDate currentDate) {
    if (organizationIds == null)
      throw new IllegalArgumentException("Organization ids cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");
    if (organizationIds.isEmpty())
      return List.of();

    List<String> ids = organizationIds.stream()
        .map(DomainID::getValue)
        .map(Object::toString)
        .distinct()
        .toList();

    return mongoRepository
        .findByActiveTrueAndDateLimitGreaterThanEqualAndOrganizationIdInOrderByDateRequestedAscIdAsc(
            currentDate, ids)
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<DonationRequest> findByRequesterPartyId(DomainID requesterPartyId) {
    if (requesterPartyId == null)
      throw new IllegalArgumentException("Requester party id cannot be null");

    return mongoRepository.findByRequesterId(requesterPartyId.getValue().toString())
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<DonationRequest> findByOrganizationId(DomainID organizationId) {
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");

    return mongoRepository.findByOrganizationId(organizationId.getValue().toString())
        .stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public void deleteById(DomainID id) {
    if (id == null)
      throw new IllegalArgumentException("Donation request id cannot be null");

    mongoRepository.deleteById(id.getValue().toString());
  }

  private DonationRequest toDomain(DonationRequestSchema schema) {
    return schema.toDomain(requesterRepository, bloodCenterRepository);
  }
}
