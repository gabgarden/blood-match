package bloodmatch.domain.repositories;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;


public interface DonationRequestRepositoryInterface {

  void save(DonationRequest request);

  Optional<DonationRequest> findById(DomainID id);

  List<DonationRequest> findActiveRequests();

  List<DonationRequest> findByRequesterPartyId(DomainID requesterPartyId);

  void deleteById(DomainID id);

}