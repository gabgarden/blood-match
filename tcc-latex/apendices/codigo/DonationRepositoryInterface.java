public interface DonationRepositoryInterface {
    void save(Donation donation);
    Optional<Donation> findById(DomainID id);
    List<Donation> findByDonorId(DomainID donorId);
    List<Donation> findCompletedDonationsOrderedByDonationDateAsc();
    List<Donation> findCompletedDonationsByOrganizationIdAndDateRange(
            DomainID organizationId,
            LocalDate startDate,
            LocalDate endDate);
    List<Donation> findPendingByOrganizationIdAndDate(
            DomainID organizationId,
            LocalDate date);
    long countByDonorId(DomainID donorId);
}
