package bloodmatch.application.usecase.donation;

import bloodmatch.application.usecase.donation.completependingdonation.CompletePendingDonationUseCase;
import bloodmatch.application.usecase.donation.completependingdonation.CompletePendingDonationUseCase.Input;
import bloodmatch.application.usecase.donation.completependingdonation.CompletePendingDonationUseCase.Output;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompletePendingDonationUseCaseTest {

  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final DonationRequestRepositoryInterface donationRequestRepository = mock(DonationRequestRepositoryInterface.class);
  private final CompletePendingDonationUseCase useCase = new CompletePendingDonationUseCase(
      donationRepository,
      donorRepository,
      donationRequestRepository,
      new DonationRequestFulfillmentService());

  @Test
  void shouldPersistFulfillmentCountAfterCompletingDonation() {
    LocalDate currentDate = LocalDate.now();

    Donor donor = new Donor(
        new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
        BloodType.of("O-"),
        70.0);

    BloodCenter bloodCenter = new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));

    Donation donation = Donation.createPending(donor, currentDate, bloodCenter, currentDate.minusDays(1));

    DonationRequest request = DonationRequest.create(
        new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
        bloodCenter,
        BloodType.of("A+"),
        1,
        currentDate.plusDays(10),
        currentDate.minusDays(1),
        Urgency.MEDIUM,
        null);

    when(donationRepository.findById(donation.getId())).thenReturn(Optional.of(donation));
    when(donationRequestRepository.findActiveRequestsByBloodCenterIds(List.of(bloodCenter.getOrganization().getId()), currentDate)).thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(List.of(bloodCenter.getOrganization().getId()))).thenReturn(List.of(donation));

    Output result = useCase.execute(
        new Input(
            donation.getId().getValue().toString(),
            currentDate,
            donor.getPerson().getId().getValue().toString()),
        currentDate);

    assertEquals(1, request.getFulfilledBloodBags());
    verify(donationRequestRepository).save(request);
    assertEquals("COMPLETED", result.status());
  }
}
