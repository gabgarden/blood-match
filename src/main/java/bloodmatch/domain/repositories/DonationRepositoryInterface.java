package bloodmatch.domain.repositories;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;




public interface DonationRepositoryInterface {

  void save(Donation donation);

  Optional<Donation> findById(DomainID id);

  List<Donation> findByDonorId(DomainID donorId);

  List<Donation> findCompletedDonationsOrderedByDonationDateAsc();

  long countByDonorId(DomainID donorId);
}