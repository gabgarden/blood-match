package bloodmatch.donation.application;

import bloodmatch.donation.application.completependingdonation.CompletePendingDonationUseCase;
import bloodmatch.donation.application.completependingdonation.CompletePendingDonationUseCase.Input;
import bloodmatch.donation.application.completependingdonation.CompletePendingDonationUseCase.Output;
import bloodmatch.donation.domain.Donation;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompletePendingDonationUseCaseTest {

  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final CompletePendingDonationUseCase useCase = new CompletePendingDonationUseCase(
      donationRepository,
      donorRepository);

  @Test
  void shouldPersistCompletedDonation() {
    LocalDate currentDate = LocalDate.now();
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    Donation donation = Donation.create(donor, bloodCenter, currentDate, null, currentDate.minusDays(1));

    when(donationRepository.findById(donation.getId())).thenReturn(Optional.of(donation));

    Output result = useCase.execute(
        new Input(
            donation.getId().getValue().toString(),
            null,
            currentDate,
            donor.getPerson().getId().getValue().toString()),
        currentDate);

    verify(donorRepository).save(donor);
    verify(donationRepository).save(donation);
    assertEquals("COMPLETED", result.status());
  }

  private Donor donor() {
    return new Donor(
        new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O-"),
        70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }
}
