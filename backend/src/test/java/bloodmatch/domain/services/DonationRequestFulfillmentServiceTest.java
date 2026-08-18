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
import static org.mockito.Mockito.mock;
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
        date,
        center,
        true,
        false,
        false);
  }

  private BloodCenter bloodCenter() {
    return new BloodCenter(new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100")));
  }

  private DomainID domainId(long value) {
    return new DomainID(new UUID(0, value));
  }
}
