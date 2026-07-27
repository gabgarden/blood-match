package bloodmatch.application.usecase.donation.createpending;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;


import java.time.LocalDate;

@Service
public class CreatePendingDonationUseCase {
  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final DonorRepositoryInterface donorRepository;
  private final DonationRepositoryInterface donationRepository;

  public CreatePendingDonationUseCase(
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository,
      DonationRepositoryInterface donationRepository) {
    this.donorRepository = donorRepository;
    this.bloodCenterRepository = bloodCenterRepository;
    this.donationRepository = donationRepository;
  }

  public Donation execute(
      DomainID donorId,
      DomainID organizationId,
      LocalDate expectedDate) {

    return execute(donorId, organizationId, expectedDate, LocalDate.now());
  }

  public Donation execute(
      DomainID donorId,
      DomainID organizationId,
      LocalDate expectedDate,
      LocalDate currentDate) {

    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");
    if (organizationId == null)
      throw new IllegalArgumentException("Organization id cannot be null");
    if (expectedDate == null)
      throw new IllegalArgumentException("Expected date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");

    Donor donor = donorRepository.findByPartyId(donorId)
        .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));
    
    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new IllegalArgumentException("Blood center role not found"));

    Donation donation = Donation.createPending(donor, expectedDate, bloodCenter, currentDate);
    donationRepository.save(donation);

    return donation;
  }
}