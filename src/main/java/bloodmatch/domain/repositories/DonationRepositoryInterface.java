package bloodmatch.domain.repositories;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;




public interface DonationRepositoryInterface {

  void save(Donation donation);

  Optional<Donation> findById(DomainID id);

  List<Donation> findByDonorId(DomainID donorId);

  boolean existsByDonorIdAndRequestId(DomainID donorId, DomainID requestId);

  boolean existsByRequestId(DomainID requestId);

  long countByDonorId(DomainID donorId);
}