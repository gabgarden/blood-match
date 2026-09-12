package bloodmatch.application.usecase.donation;

import bloodmatch.application.usecase.donation.completependingdonation.CompletePendingDonationUseCase;
import bloodmatch.application.usecase.donation.completependingdonation.CompletePendingDonationUseCase.Input;
import bloodmatch.application.usecase.donation.completependingdonation.CompletePendingDonationUseCase.Output;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
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
