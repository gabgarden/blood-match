package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.DonationMongoRepository;
import bloodmatch.infra.persistence.schema.DonationSchema;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DonationRepositoryImpl implements DonationRepositoryInterface {

  private final DonationMongoRepository mongoRepository;
  private final DonorRepositoryInterface donorRepository;
  private final DonationRequestRepositoryInterface donationRequestRepository;
  private final PartyRepositoryInterface partyRepository;

  public DonationRepositoryImpl(
      DonationMongoRepository mongoRepository,
      DonorRepositoryInterface donorRepository,
      DonationRequestRepositoryInterface donationRequestRepository,
      PartyRepositoryInterface partyRepository) {
    this.mongoRepository = mongoRepository;
    this.donorRepository = donorRepository;
    this.donationRequestRepository = donationRequestRepository;
    this.partyRepository = partyRepository;
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
  public boolean existsByDonorIdAndRequestId(DomainID donorId, DomainID requestId) {
    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");
    if (requestId == null)
      throw new IllegalArgumentException("Request id cannot be null");

    return mongoRepository.existsByDonorPersonIdAndRequestId(
        donorId.getValue().toString(),
        requestId.getValue().toString());
  }

  @Override
  public boolean existsByRequestId(DomainID requestId) {
    if (requestId == null)
      throw new IllegalArgumentException("Request id cannot be null");

    return mongoRepository.existsByRequestId(requestId.getValue().toString());
  }

  @Override
  public long countByDonorId(DomainID donorId) {
    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");

    return mongoRepository.countByDonorPersonId(donorId.getValue().toString());
  }

  private Donation toDomain(DonationSchema schema) {
    return schema.toDomain(donorRepository, donationRequestRepository, partyRepository);
  }
}
