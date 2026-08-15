package bloodmatch.application.usecase.donor.getsummary;

import bloodmatch.application.usecase.donor.getsummary.GetDonorSummaryUseCase.Input;
import bloodmatch.application.usecase.donor.getsummary.GetDonorSummaryUseCase.Output;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
