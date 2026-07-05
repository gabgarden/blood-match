package bloodmatch.application.usecase.donation.createexternal;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class CreateExternalDonationUseCase {

  private final DonorRepositoryInterface donorRepository;
  private final PartyRepositoryInterface partyRepository;
  private final DonationRepositoryInterface donationRepository;

  public CreateExternalDonationUseCase(
      DonorRepositoryInterface donorRepository,
      PartyRepositoryInterface partyRepository,
      DonationRepositoryInterface donationRepository) {
    this.donorRepository = donorRepository;
    this.partyRepository = partyRepository;
    this.donationRepository = donationRepository;
  }

  public Donation execute(
      DomainID donorId,
      DomainID bloodCenterId,
      LocalDate donationDate) {

    if (donorId == null)
      throw new IllegalArgumentException("Donor id cannot be null");
    if (bloodCenterId == null)
      throw new IllegalArgumentException("Blood center id cannot be null");
    if (donationDate == null)
      throw new IllegalArgumentException("Donation date cannot be null");

    Donor donor = donorRepository.findByPartyId(donorId)
        .orElseThrow(() -> new IllegalArgumentException("Donor role not found"));

    Organization organization = partyRepository.findById(bloodCenterId)
        .filter(Organization.class::isInstance)
        .map(Organization.class::cast)
        .orElseThrow(() -> new IllegalArgumentException("Blood center organization not found"));

    BloodCenter bloodCenter = new BloodCenter(organization);

    Donation donation = Donation.registerExternalDonation(donor, donationDate, bloodCenter, LocalDate.now());
    donor.registerDonation(donationDate, LocalDate.now());
    donorRepository.save(donor);
    donationRepository.save(donation);

    return donation;
  }
}
