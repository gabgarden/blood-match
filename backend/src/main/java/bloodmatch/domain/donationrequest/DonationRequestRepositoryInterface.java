package bloodmatch.domain.donationrequest;

import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.List;
import java.util.Optional;

public interface DonationRequestRepositoryInterface {

  void save(DonationRequest request);

  Optional<DonationRequest> findById(DomainID id);

  List<DonationRequest> findActiveRequests();

  List<DonationRequest> findActiveRequestsForDonor(
      BloodType donorBloodType,
      Address donorAddress,
      double maxDistanceInKm,
      java.time.LocalDate currentDate);

  List<DonationRequest> findActiveRequestsByOrganizationIds(
      List<DomainID> organizationIds,
      java.time.LocalDate currentDate);

  List<DonationRequest> findByRequesterPartyId(DomainID requesterPartyId);

  void deleteById(DomainID id);

}
