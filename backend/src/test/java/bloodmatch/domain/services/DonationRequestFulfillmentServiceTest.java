package bloodmatch.domain.services;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.services.records.DonationRequestFulfillmentStatusRecord;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonationRequestFulfillmentServiceTest {

  private final LocalDate currentDate = LocalDate.of(2026, 4, 23);
  private final DonationRequestRepositoryInterface requestRepository =
      mock(DonationRequestRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository =
      mock(DonationRepositoryInterface.class);
  private final DonationRequestFulfillmentService service =
      new DonationRequestFulfillmentService(requestRepository, donationRepository);

  @Test
  void fillAllocatesCompatibleDonationToTheRequest() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 1);
    Donation donation = donation(1, center, currentDate);
    stubSnapshot(center, List.of(request), List.of(donation), currentDate.minusDays(2), currentDate);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fill(center, currentDate, currentDate);

    assertEquals(1, result.get(request.getId()).fulfilledBloodBags());
    assertTrue(result.get(request.getId()).goalReached());
  }

  @Test
  void fillLoadsTheBloodCenterSnapshotFromTheRepositories() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 1);
    Donation donation = donation(1, center, currentDate);
    stubSnapshot(center, List.of(request), List.of(donation), currentDate.minusDays(2), currentDate);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fill(center, currentDate, currentDate);

    assertEquals(1, result.get(request.getId()).fulfilledBloodBags());
  }

  @Test
  void fillRejectsInvertedDateWindow() {
    BloodCenter center = bloodCenter();

    assertThrows(
        IllegalArgumentException.class,
        () -> service.fill(center, currentDate, currentDate.minusDays(1)));
  }

  @Test
  void fillListLoadsEveryOrganizationInTwoQueries() {
    BloodCenter firstCenter = bloodCenter();
    BloodCenter secondCenter = bloodCenter();
    DonationRequest first = request(1, firstCenter, currentDate.minusDays(2), currentDate.plusDays(5), 1);
    DonationRequest second = request(2, secondCenter, currentDate.minusDays(2), currentDate.plusDays(5), 1);

    when(requestRepository.findByOrganizationIds(anyList())).thenReturn(List.of(first, second));
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(
            anyList(), any(), any()))
        .thenReturn(List.of(
            donation(1, firstCenter, currentDate),
            donation(2, secondCenter, currentDate)));

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fill(List.of(first, second), currentDate, currentDate);

    assertEquals(1, result.get(first.getId()).fulfilledBloodBags());
    assertEquals(1, result.get(second.getId()).fulfilledBloodBags());
  }

  @Test
  void fillWithAsOfDateLoadsOnlyActiveRequestsAtTheBloodCenter() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 1);
    stubActiveSnapshot(List.of(request), List.of(donation(1, center, currentDate)));

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fill(center, currentDate);

    assertEquals(1, result.get(request.getId()).fulfilledBloodBags());
  }

  @Test
  void fillWithAsOfDateLoadsOnlyActiveRequestsForTheRequestList() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 1);
    stubActiveSnapshot(List.of(request), List.of(donation(1, center, currentDate)));

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fill(List.of(request), currentDate);

    assertEquals(1, result.get(request.getId()).fulfilledBloodBags());
  }

  @Test
  void fillAmongAllocatesOnlyTheProvidedRequestsWithoutReloadingCenterHistory() {
    BloodCenter center = bloodCenter();
    DonationRequest candidate = request(2, center, currentDate.minusDays(1), currentDate.plusDays(5), 1);
    stubDonations(List.of(donation(1, center, currentDate)), currentDate.minusDays(1), currentDate);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fillAmong(List.of(candidate), currentDate);

    assertEquals(1, result.get(candidate.getId()).fulfilledBloodBags());
    verify(requestRepository, never()).findByOrganizationIds(anyList());
    verify(requestRepository, never()).findActiveRequestsByOrganizationIds(anyList(), any());
    verify(requestRepository, never()).findByOrganizationId(any());
  }

  @Test
  void fillAmongUsesTheOldestProvidedRequestAsTheDonationWindowStart() {
    BloodCenter center = bloodCenter();
    DonationRequest older = request(1, center, currentDate.minusDays(10), currentDate.plusDays(5), 1);
    DonationRequest newer = request(2, center, currentDate.minusDays(1), currentDate.plusDays(5), 1);
    stubDonations(
        List.of(donation(1, center, currentDate.minusDays(9))),
        currentDate.minusDays(10),
        currentDate);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fillAmong(List.of(newer, older), currentDate);

    assertEquals(1, result.get(older.getId()).fulfilledBloodBags());
    assertEquals(0, result.get(newer.getId()).fulfilledBloodBags());
  }

  @Test
  void fillReturnsEmptyMapWhenBloodCenterHasNoRequests() {
    BloodCenter center = bloodCenter();
    when(requestRepository.findActiveRequestsByOrganizationIds(anyList(), any())).thenReturn(List.of());

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fill(center, currentDate);

    assertTrue(result.isEmpty());
  }

  private void stubActiveSnapshot(List<DonationRequest> requests, List<Donation> donations) {
    when(requestRepository.findActiveRequestsByOrganizationIds(anyList(), any())).thenReturn(requests);
    stubDonations(donations, currentDate.minusDays(2), currentDate);
  }

  private void stubDonations(List<Donation> donations, LocalDate from, LocalDate to) {
    when(donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(anyList(), eq(from), eq(to)))
        .thenReturn(donations);
  }

  private void stubSnapshot(
      BloodCenter center,
      List<DonationRequest> requests,
      List<Donation> donations,
      LocalDate from,
      LocalDate to) {
    DomainID organizationId = center.getOrganization().getId();
    when(requestRepository.findByOrganizationId(organizationId)).thenReturn(requests);
    when(donationRepository.findCompletedDonationsByOrganizationIdAndDateRange(
            organizationId, from, to))
        .thenReturn(donations);
  }

  private DonationRequest request(
      long id,
      BloodCenter center,
      LocalDate requestedAt,
      LocalDate limit,
      int goal) {
    return DonationRequest.reconstitute(
        domainId(id),
        new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
        center,
        BloodType.of("A+"),
        goal,
        requestedAt,
        limit,
        true,
        Urgency.MEDIUM,
        null,
        null);
  }

  private Donation donation(long id, BloodCenter center, LocalDate date) {
    return Donation.reconstitute(
        domainId(id),
        new Donor(
            new Person("Donor", new PhoneNumber("11988887777"), new CPF("98765432100"), LocalDate.of(1990, 1, 1)),
            BloodType.of("O-"),
            70.0),
        null,
        date,
        null,
        center);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }

  private DomainID domainId(long value) {
    return new DomainID(new UUID(0, value));
  }
}
