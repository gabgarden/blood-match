package bloodmatch.donation.application.gethistory;

import bloodmatch.donation.domain.Donation;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetDonorDonationHistoryUseCaseTest {

  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final GetDonorDonationHistoryUseCase useCase = new GetDonorDonationHistoryUseCase(donationRepository);

  @Test
  void shouldReturnDonationsOrderedByReferenceDateDesc() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    DomainID personId = donor.getPerson().getId();

    LocalDate d1 = LocalDate.of(2026, 1, 10);
    LocalDate d2 = LocalDate.of(2026, 3, 15);

    Donation older = Donation.reconstitute(DomainID.generate(), donor, null, d1, null, bloodCenter, null, 1L);
    Donation newer = Donation.reconstitute(DomainID.generate(), donor, null, d2, null, bloodCenter, null, 2L);

    when(donationRepository.findByDonorId(personId)).thenReturn(List.of(older, newer));

    List<GetDonorDonationHistoryUseCase.OutputItem> result = useCase.execute(
        new GetDonorDonationHistoryUseCase.Input(personId.getValue().toString()));

    assertEquals(2, result.size());
    // Most recent first
    assertEquals(d2, result.get(0).date());
    assertEquals("COMPLETED", result.get(0).status());
    assertEquals("Hemocentro Central", result.get(0).location());
    assertEquals(2L, result.get(0).version());

    assertEquals(d1, result.get(1).date());
  }

  @Test
  void shouldValidateInput() {
    assertThrows(ValidationException.class, () -> useCase.execute(null));
    assertThrows(ValidationException.class, () -> useCase.execute(new GetDonorDonationHistoryUseCase.Input("not-a-uuid")));
    assertThrows(ValidationException.class, () -> useCase.execute(new GetDonorDonationHistoryUseCase.Input("")));
  }

  private Donor donor() {
    return new Donor(
        new Person("Doador", new PhoneNumber("11988887777"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O+"),
        70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Hemocentro Central", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }
}
