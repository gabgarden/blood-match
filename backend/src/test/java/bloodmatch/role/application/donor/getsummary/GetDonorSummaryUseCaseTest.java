package bloodmatch.role.application.donor.getsummary;

import bloodmatch.role.application.donor.getsummary.GetDonorSummaryUseCase.Input;
import bloodmatch.role.application.donor.getsummary.GetDonorSummaryUseCase.Output;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.person.donor.DonorRepositoryInterface;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.shared.domain.valueObjects.Address;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetDonorSummaryUseCaseTest {

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final GetDonorSummaryUseCase useCase = new GetDonorSummaryUseCase(donorRepository, donationRepository);

  @Test
  void shouldIncludeWeightAndWeightUpdatedAt() {
    Person person = new Person(
        "Ana Silva",
        new PhoneNumber("11988887777"),
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1));
    person.changeAddress(new Address("Rua A", "Sao Paulo", "SP", "01000-000"));
    Donor donor = new Donor(person, BloodType.of("O+"), 72.5);

    when(donorRepository.findByPartyId(person.getId())).thenReturn(Optional.of(donor));
    when(donationRepository.countByDonorId(person.getId())).thenReturn(2L);

    Output output = useCase.execute(new Input(person.getId().getValue().toString()), LocalDate.now());

    assertEquals(72.5, output.weight());
    assertEquals(LocalDate.now(), output.weightUpdatedAt());
    assertEquals("Ana Silva", output.donorName());
    assertEquals("O+", output.bloodType());
    assertEquals(8L, output.livesImpacted());
  }

  @Test
  void shouldValidateInputAndNullChecks() {
    org.junit.jupiter.api.Assertions.assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(null));
    org.junit.jupiter.api.Assertions.assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(new Input("id"), null));
  }

  @Test
  void shouldThrowNotFoundWhenDonorMissing() {
    when(donorRepository.findByPartyId(any())).thenReturn(Optional.empty());
    org.junit.jupiter.api.Assertions.assertThrows(bloodmatch.shared.application.exception.NotFoundException.class, () -> useCase.execute(
        new Input(bloodmatch.shared.domain.valueObjects.DomainID.generate().getValue().toString()), LocalDate.now()));
  }

  @Test
  void shouldCalculateRemainingDaysCorrectlyAndSupportOverload() {
    LocalDate today = LocalDate.now();
    Person person = new Person(
        "Carlos",
        new PhoneNumber("11988887777"),
        new CPF("12345678901"),
        LocalDate.of(1990, 1, 1));
    Donor donor = new Donor(person, BloodType.of("A+"), 65.0);

    // 30 days ago -> 60 days remaining
    donor.registerDonation(today.minusDays(30), today.minusDays(30));

    when(donorRepository.findByPartyId(person.getId())).thenReturn(Optional.of(donor));
    when(donationRepository.countByDonorId(person.getId())).thenReturn(1L);

    Output output1 = useCase.execute(new Input(person.getId().getValue().toString()), today);
    assertEquals(60, output1.daysRemaining());

    // 100 days ago -> 0 days remaining
    donor.registerDonation(today.minusDays(100), today.minusDays(100));
    Output output2 = useCase.execute(new Input(person.getId().getValue().toString()));
    assertEquals(0, output2.daysRemaining());
  }
}
