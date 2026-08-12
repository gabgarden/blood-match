public interface DonationRepositoryInterface {
    void save(Donation donation);
    Optional<Donation> findById(DomainID id);
    List<Donation> findByDonorId(DomainID donorId);
    List<Donation> findCompletedDonationsOrderedByDonationDateAsc();
    List<Donation> findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
        List<DomainID> organizationIds);
    long countByDonorId(DomainID donorId);
}
