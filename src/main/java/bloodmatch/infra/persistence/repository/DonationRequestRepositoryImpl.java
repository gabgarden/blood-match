package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.repositories.RequesterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.DonationRequestMongoRepository;
import bloodmatch.infra.persistence.schema.DonationRequestSchema;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DonationRequestRepositoryImpl implements DonationRequestRepositoryInterface {

  private final DonationRequestMongoRepository mongoRepository;
  private final RequesterRepositoryInterface requesterRepository;
  private final PartyRepositoryInterface partyRepository;

  public DonationRequestRepositoryImpl(
      DonationRequestMongoRepository mongoRepository,
      RequesterRepositoryInterface requesterRepository,
      PartyRepositoryInterface partyRepository) {
    this.mongoRepository = mongoRepository;
    this.requesterRepository = requesterRepository;
    this.partyRepository = partyRepository;
  }

  @Override
  public void save(DonationRequest request) {
    if (request == null)
      throw new IllegalArgumentException("DonationRequest cannot be null");

    DonationRequestSchema schema = new DonationRequestSchema(request);
    mongoRepository.save(schema);
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
    return mongoRepository.findByActive(true)
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
  public void deleteById(DomainID id) {
    if (id == null)
      throw new IllegalArgumentException("Donation request id cannot be null");

    mongoRepository.deleteById(id.getValue().toString());
  }

  private DonationRequest toDomain(DonationRequestSchema schema) {
    return schema.toDomain(requesterRepository, partyRepository);
  }
}
