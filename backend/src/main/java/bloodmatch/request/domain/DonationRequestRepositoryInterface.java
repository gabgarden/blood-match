package bloodmatch.request.domain;

import bloodmatch.shared.domain.valueObjects.Address;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.DomainID;

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

  List<DonationRequest> findByOrganizationId(DomainID organizationId);

  List<DonationRequest> findByOrganizationIds(List<DomainID> organizationIds);

  void deleteById(DomainID id);

}
