package bloodmatch.donation.application.reschedulependingdonation;

import bloodmatch.donation.domain.Donation;
import bloodmatch.donation.domain.DonationRepositoryInterface;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.shared.application.exception.ConcurrencyException;
import bloodmatch.shared.application.exception.ForbiddenException;
import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReschedulePendingDonationUseCaseTest {

  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final ReschedulePendingDonationUseCase useCase = new ReschedulePendingDonationUseCase(donationRepository);

  private final LocalDate today = LocalDate.of(2026, 4, 10);

  @Test
  void shouldReschedulePendingDonationSuccessfully() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    DomainID donationId = DomainID.generate();
    Donation donation = Donation.reconstitute(
        donationId, donor, today.plusDays(3), null, null, bloodCenter, null, 1L);

    when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));

    LocalDate newDate = today.plusDays(7);
    String actorId = donor.getPerson().getId().getValue().toString();
    ReschedulePendingDonationUseCase.Input input = new ReschedulePendingDonationUseCase.Input(
        donationId.getValue().toString(), 1L, newDate, actorId);

    var output = useCase.execute(input, today);

    assertEquals(donationId.getValue().toString(), output.id());
    assertEquals(newDate, output.expectedDate());
    assertEquals("PENDING", output.status());
    verify(donationRepository).save(donation);
  }

  @Test
  void shouldThrowConcurrencyExceptionOnVersionConflict() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    DomainID donationId = DomainID.generate();
    Donation donation = Donation.reconstitute(
        donationId, donor, today.plusDays(3), null, null, bloodCenter, null, 2L);

    when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));

    String actorId = donor.getPerson().getId().getValue().toString();
    // Request sends version 1L but entity has version 2L (simultaneous update conflict)
    ReschedulePendingDonationUseCase.Input input = new ReschedulePendingDonationUseCase.Input(
        donationId.getValue().toString(), 1L, today.plusDays(5), actorId);

    assertThrows(ConcurrencyException.class, () -> useCase.execute(input, today));
  }

  @Test
  void shouldValidateInputAndNulls() {
    assertThrows(ValidationException.class, () -> useCase.execute(null, today));
    assertThrows(ValidationException.class, () -> useCase.execute(new ReschedulePendingDonationUseCase.Input("id", 1L, today, "actor"), null));

    DomainID donationId = DomainID.generate();
    // Missing newExpectedDate
    var missingDate = new ReschedulePendingDonationUseCase.Input(donationId.getValue().toString(), 1L, null, "actor");
    assertThrows(ValidationException.class, () -> useCase.execute(missingDate, today));
  }

  @Test
  void shouldThrowNotFoundWhenDonationDoesNotExist() {
    DomainID donationId = DomainID.generate();
    when(donationRepository.findById(donationId)).thenReturn(Optional.empty());

    var input = new ReschedulePendingDonationUseCase.Input(donationId.getValue().toString(), 1L, today.plusDays(1), "actor");
    assertThrows(NotFoundException.class, () -> useCase.execute(input, today));
  }

  @Test
  void shouldThrowForbiddenWhenActorDoesNotOwnDonation() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    DomainID donationId = DomainID.generate();
    Donation donation = Donation.reconstitute(
        donationId, donor, today.plusDays(3), null, null, bloodCenter, null, 1L);

    when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));

    var forbiddenInput = new ReschedulePendingDonationUseCase.Input(
        donationId.getValue().toString(), 1L, today.plusDays(5), DomainID.generate().getValue().toString());

    assertThrows(ForbiddenException.class, () -> useCase.execute(forbiddenInput, today));
  }

  @Test
  void shouldExecuteWithDefaultCurrentDate() {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    DomainID donationId = DomainID.generate();
    Donation donation = Donation.reconstitute(
        donationId, donor, LocalDate.now().plusDays(3), null, null, bloodCenter, null, 1L);

    when(donationRepository.findById(donationId)).thenReturn(Optional.of(donation));

    String actorId = donor.getPerson().getId().getValue().toString();
    var input = new ReschedulePendingDonationUseCase.Input(
        donationId.getValue().toString(), 1L, LocalDate.now().plusDays(6), actorId);

    var output = useCase.execute(input);
    assertEquals("PENDING", output.status());
  }

  private Donor donor() {
    return new Donor(
        new Person("Doador", new PhoneNumber("11988887777"), new CPF("12345678901"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O+"),
        70.0);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Hemocentro", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }
}
