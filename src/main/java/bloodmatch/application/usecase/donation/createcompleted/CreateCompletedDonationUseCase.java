package bloodmatch.application.usecase.donation.createcompleted;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.repositories.BloodCenterRepositoryInterface;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CreateCompletedDonationUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final DonationRepositoryInterface donationRepository;

  public CreateCompletedDonationUseCase(
      DonorRepositoryInterface donorRepository,
      BloodCenterRepositoryInterface bloodCenterRepository,
      DonationRepositoryInterface donationRepository) {
    if (donorRepository == null)
      throw new IllegalArgumentException("DonorRepository cannot be null");
    if (bloodCenterRepository == null)
      throw new IllegalArgumentException("BloodCenterRepository cannot be null");
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");
    this.donorRepository = donorRepository;
    this.bloodCenterRepository = bloodCenterRepository;
    this.donationRepository = donationRepository;
  }

  public Donation execute(
      DomainID personId,
      DomainID bloodCenterId,
      LocalDate donationDate) {

    if (personId == null)
      throw new IllegalArgumentException("Person id cannot be null");
    if (bloodCenterId == null)
      throw new IllegalArgumentException("Blood center id cannot be null");
    if (donationDate == null)
      throw new IllegalArgumentException("Donation date cannot be null");

    Donor donor = donorRepository.findByPartyId(personId)
        .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));

    BloodCenter bloodCenter = bloodCenterRepository.findByPartyId(bloodCenterId)
        .orElseThrow(() -> new IllegalArgumentException("Blood center role not found"));

    Donation donation = Donation.registerExternalDonation(donor, donationDate, bloodCenter, LocalDate.now());
    donor.registerDonation(donationDate, LocalDate.now());
    donorRepository.save(donor);
    donationRepository.save(donation);

    return donation;
  }
}