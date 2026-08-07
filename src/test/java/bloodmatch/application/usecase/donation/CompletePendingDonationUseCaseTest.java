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
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    Fixture fixture = fixture(currentDate);

    when(donationRepository.findById(fixture.donation.getId())).thenReturn(Optional.of(fixture.donation));
    when(donationRequestRepository.findActiveRequestsByBloodCenterIds(
            List.of(fixture.bloodCenter.getOrganization().getId()), currentDate))
        .thenReturn(List.of(fixture.request));
    when(donationRepository.findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(
            List.of(fixture.bloodCenter.getOrganization().getId())))
        .thenReturn(List.of(fixture.donation));

    Output result = useCase.execute(
        new Input(
            fixture.donation.getId().getValue().toString(),
            currentDate,
            fixture.donor.getPerson().getId().getValue().toString()),
        currentDate);

    assertEquals(1, fixture.request.getFulfilledBloodBags());
    verify(donationRequestRepository).save(fixture.request);
    assertEquals("COMPLETED", result.status());
  }

  @Test
  void skipsRequestPersistenceWhenThereAreNoActiveRequests() {
    LocalDate currentDate = LocalDate.now();
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    Donation donation = Donation.createPending(donor, currentDate, bloodCenter, currentDate.minusDays(1));

    when(donationRepository.findById(donation.getId())).thenReturn(Optional.of(donation));
    when(donationRequestRepository.findActiveRequestsByBloodCenterIds(
            List.of(bloodCenter.getOrganization().getId()), currentDate))
        .thenReturn(List.of());

    useCase.execute(
        new Input(
            donation.getId().getValue().toString(),
            currentDate,
            donor.getPerson().getId().getValue().toString()),
        currentDate);

    verify(donationRepository).save(donation);
    verify(donationRepository, never())
        .findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(anyList());
    verify(donationRequestRepository, never()).save(any(DonationRequest.class));
  }

  @Test
  void redistributesHistoricalPoolAcrossActiveRequestsInFifoOrder() {
    LocalDate currentDate = LocalDate.now();
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    Donation pending = Donation.createPending(donor, currentDate, bloodCenter, currentDate.minusDays(1));
    DonationRequest oldest = request(bloodCenter, currentDate.minusDays(3), currentDate.plusDays(10), 1, 0);
    DonationRequest newest = request(bloodCenter, currentDate.minusDays(1), currentDate.plusDays(10), 1, 0);
    Donation historical = Donation.reconstitute(
        new DomainID(new UUID(0, 1)), donor, currentDate.minusDays(1), bloodCenter, true, false, false);

    when(donationRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
    when(donationRequestRepository.findActiveRequestsByBloodCenterIds(
            List.of(bloodCenter.getOrganization().getId()), currentDate))
        .thenReturn(List.of(newest, oldest));
    when(donationRepository.findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(
            List.of(bloodCenter.getOrganization().getId())))
        .thenReturn(List.of(historical, pending));

    useCase.execute(
        new Input(
            pending.getId().getValue().toString(),
            currentDate,
            donor.getPerson().getId().getValue().toString()),
        currentDate);

    assertEquals(1, oldest.getFulfilledBloodBags());
    assertEquals(1, newest.getFulfilledBloodBags());
    verify(donationRequestRepository, times(2)).save(any(DonationRequest.class));
  }

  @Test
  void overwritesStaleFulfilledCounterOnRefresh() {
    LocalDate currentDate = LocalDate.now();
    Fixture fixture = fixture(currentDate);
    fixture.request.setFulfilledBloodBags(99);

    when(donationRepository.findById(fixture.donation.getId())).thenReturn(Optional.of(fixture.donation));
    when(donationRequestRepository.findActiveRequestsByBloodCenterIds(
            List.of(fixture.bloodCenter.getOrganization().getId()), currentDate))
        .thenReturn(List.of(fixture.request));
    when(donationRepository.findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(
            List.of(fixture.bloodCenter.getOrganization().getId())))
        .thenReturn(List.of(fixture.donation));

    useCase.execute(
        new Input(
            fixture.donation.getId().getValue().toString(),
            currentDate,
            fixture.donor.getPerson().getId().getValue().toString()),
        currentDate);

    assertEquals(1, fixture.request.getFulfilledBloodBags());
    ArgumentCaptor<DonationRequest> captor = ArgumentCaptor.forClass(DonationRequest.class);
    verify(donationRequestRepository).save(captor.capture());
    assertEquals(1, captor.getValue().getFulfilledBloodBags());
  }

  private Fixture fixture(LocalDate currentDate) {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    Donation donation = Donation.createPending(donor, currentDate, bloodCenter, currentDate.minusDays(1));
    DonationRequest request = request(bloodCenter, currentDate.minusDays(1), currentDate.plusDays(10), 1, 0);
    return new Fixture(donor, bloodCenter, donation, request);
  }

  private DonationRequest request(
      BloodCenter bloodCenter,
      LocalDate requestedAt,
      LocalDate limit,
      int goal,
      int fulfilled) {
    return DonationRequest.reconstitute(
        DomainID.generate(),
        new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
        bloodCenter,
        BloodType.of("A+"),
        goal,
        requestedAt,
        limit,
        true,
        Urgency.MEDIUM,
        null,
        fulfilled,
        null);
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

  private record Fixture(Donor donor, BloodCenter bloodCenter, Donation donation, DonationRequest request) {
  }
}
