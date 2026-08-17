package bloodmatch.application.usecase.donation;

import bloodmatch.application.usecase.donation.createcompleted.CreateCompletedDonationUseCase;
import bloodmatch.application.usecase.donation.createcompleted.CreateCompletedDonationUseCase.Input;
import bloodmatch.application.usecase.donation.createcompleted.CreateCompletedDonationUseCase.Output;
import bloodmatch.application.usecase.donation.fulfillment.DonationRequestFulfillmentRefresher;
import bloodmatch.application.usecase.donation.fulfillment.OrganizationFulfillmentLock;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
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

class CreateCompletedDonationUseCaseTest {

  private final DonorRepositoryInterface donorRepository = mock(DonorRepositoryInterface.class);
  private final BloodCenterRepositoryInterface bloodCenterRepository = mock(BloodCenterRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final DonationRequestRepositoryInterface donationRequestRepository = mock(DonationRequestRepositoryInterface.class);
  private final DonationRequestFulfillmentRefresher fulfillmentRefresher = new DonationRequestFulfillmentRefresher(
      donationRequestRepository,
      donationRepository,
      new DonationRequestFulfillmentService());
  private final CreateCompletedDonationUseCase useCase = new CreateCompletedDonationUseCase(
      donorRepository,
      bloodCenterRepository,
      donationRepository,
      fulfillmentRefresher,
      new OrganizationFulfillmentLock());

  @Test
  void shouldPersistFulfillmentCountAfterRegisteringCompletedDonation() {
    LocalDate currentDate = LocalDate.now();
    Fixture fixture = fixture(currentDate, 1);

    when(donorRepository.findByPartyId(fixture.donor.getPerson().getId())).thenReturn(Optional.of(fixture.donor));
    when(bloodCenterRepository.findByPartyId(fixture.bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(fixture.bloodCenter));
    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(fixture.bloodCenter.getOrganization().getId()), currentDate))
        .thenReturn(List.of(fixture.request));
    when(donationRepository.findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
            List.of(fixture.bloodCenter.getOrganization().getId())))
        .thenReturn(List.of(Donation.registerExternalDonation(fixture.donor, currentDate, fixture.bloodCenter, currentDate)));

    Output result = useCase.execute(
        new Input(
            fixture.donor.getPerson().getId().getValue().toString(),
            fixture.bloodCenter.getOrganization().getId().getValue().toString(),
            currentDate),
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

    when(donorRepository.findByPartyId(donor.getPerson().getId())).thenReturn(Optional.of(donor));
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));
    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(bloodCenter.getOrganization().getId()), currentDate))
        .thenReturn(List.of());

    useCase.execute(
        new Input(
            donor.getPerson().getId().getValue().toString(),
            bloodCenter.getOrganization().getId().getValue().toString(),
            currentDate),
        currentDate);

    verify(donationRepository).save(any(Donation.class));
    verify(donationRepository, never())
        .findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(anyList());
    verify(donationRequestRepository, never()).save(any(DonationRequest.class));
  }

  @Test
  void redistributesHistoricalPoolAcrossActiveRequestsInFifoOrder() {
    LocalDate currentDate = LocalDate.now();
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    DonationRequest oldest = request(bloodCenter, currentDate.minusDays(3), currentDate.plusDays(10), 1, 0);
    DonationRequest newest = request(bloodCenter, currentDate.minusDays(1), currentDate.plusDays(10), 1, 0);
    Donation olderDonation = Donation.reconstitute(
        new DomainID(new UUID(0, 1)), donor, currentDate.minusDays(1), bloodCenter, true, false, false);
    Donation newerDonation = Donation.reconstitute(
        new DomainID(new UUID(0, 2)), donor, currentDate, bloodCenter, true, false, false);

    when(donorRepository.findByPartyId(donor.getPerson().getId())).thenReturn(Optional.of(donor));
    when(bloodCenterRepository.findByPartyId(bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(bloodCenter));
    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(bloodCenter.getOrganization().getId()), currentDate))
        .thenReturn(List.of(newest, oldest));
    when(donationRepository.findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
            List.of(bloodCenter.getOrganization().getId())))
        .thenReturn(List.of(olderDonation, newerDonation));

    useCase.execute(
        new Input(
            donor.getPerson().getId().getValue().toString(),
            bloodCenter.getOrganization().getId().getValue().toString(),
            currentDate),
        currentDate);

    assertEquals(1, oldest.getFulfilledBloodBags());
    assertEquals(1, newest.getFulfilledBloodBags());
    verify(donationRequestRepository, times(2)).save(any(DonationRequest.class));
  }

  @Test
  void overwritesStaleFulfilledCounterOnRefresh() {
    LocalDate currentDate = LocalDate.now();
    Fixture fixture = fixture(currentDate, 1);
    fixture.request.setFulfilledBloodBags(99);

    when(donorRepository.findByPartyId(fixture.donor.getPerson().getId())).thenReturn(Optional.of(fixture.donor));
    when(bloodCenterRepository.findByPartyId(fixture.bloodCenter.getOrganization().getId()))
        .thenReturn(Optional.of(fixture.bloodCenter));
    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(fixture.bloodCenter.getOrganization().getId()), currentDate))
        .thenReturn(List.of(fixture.request));
    when(donationRepository.findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
            List.of(fixture.bloodCenter.getOrganization().getId())))
        .thenReturn(List.of(Donation.registerExternalDonation(fixture.donor, currentDate, fixture.bloodCenter, currentDate)));

    useCase.execute(
        new Input(
            fixture.donor.getPerson().getId().getValue().toString(),
            fixture.bloodCenter.getOrganization().getId().getValue().toString(),
            currentDate),
        currentDate);

    assertEquals(1, fixture.request.getFulfilledBloodBags());
    ArgumentCaptor<DonationRequest> captor = ArgumentCaptor.forClass(DonationRequest.class);
    verify(donationRequestRepository).save(captor.capture());
    assertEquals(1, captor.getValue().getFulfilledBloodBags());
  }

  private Fixture fixture(LocalDate currentDate, int goal) {
    Donor donor = donor();
    BloodCenter bloodCenter = bloodCenter();
    DonationRequest request = request(bloodCenter, currentDate.minusDays(1), currentDate.plusDays(10), goal, 0);
    return new Fixture(donor, bloodCenter, request);
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

  private record Fixture(Donor donor, BloodCenter bloodCenter, DonationRequest request) {
  }
}
