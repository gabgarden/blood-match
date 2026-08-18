package bloodmatch.domain.donation;

import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DonationRepositoryInterface {

  void save(Donation donation);

  Optional<Donation> findById(DomainID id);

  List<Donation> findByDonorId(DomainID donorId);

  List<Donation> findCompletedDonationsOrderedByDonationDateAsc();

  List<Donation> findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
      List<DomainID> organizationIds);

  List<Donation> findCompletedDonationsByOrganizationIdAndDateRange(
      DomainID organizationId,
      LocalDate startDate,
      LocalDate endDate);

  List<Donation> findCompletedDonationsByOrganizationIdsAndDateRange(
      List<DomainID> organizationIds,
      LocalDate startDate,
      LocalDate endDate);

  List<Donation> findPendingByOrganizationId(DomainID organizationId);

  List<Donation> findPendingByOrganizationIdAndDate(DomainID organizationId, LocalDate date);

  List<Donation> findPendingByOrganizationIdAndDateRange(
      DomainID organizationId,
      LocalDate from,
      LocalDate to);

  long countByDonorId(DomainID donorId);
}
