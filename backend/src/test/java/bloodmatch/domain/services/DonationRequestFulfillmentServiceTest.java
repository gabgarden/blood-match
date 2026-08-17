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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fill(center, currentDate.minusDays(10), currentDate, List.of(request), List.of(donation));

    assertEquals(1, result.get(request.getId()).fulfilledBloodBags());
    assertTrue(result.get(request.getId()).goalReached());
  }

  @Test
  void fillDoesNotMutateTheRequest() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 2);
    Donation donation = donation(1, center, currentDate);

    service.fill(center, currentDate.minusDays(10), currentDate, List.of(request), List.of(donation));

    assertEquals(2, request.getGoalBloodBags());
  }

  @Test
  void fillIgnoresDonationsOutsideTheSnapshotWindow() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(20), currentDate.plusDays(5), 1);
    Donation tooEarly = donation(1, center, currentDate.minusDays(10));
    Donation tooLate = donation(2, center, currentDate.plusDays(1));

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fill(center, currentDate.minusDays(2), currentDate, List.of(request), List.of(tooEarly, tooLate));

    assertEquals(0, result.get(request.getId()).fulfilledBloodBags());
    assertFalse(result.get(request.getId()).goalReached());
  }

  @Test
  void fillLoadsSnapshotFromRepositories() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 1);
    Donation donation = donation(1, center, currentDate);
    DomainID organizationId = center.getOrganization().getId();

    when(requestRepository.findByOrganizationId(organizationId)).thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsByOrganizationIdAndDateRange(
            organizationId, currentDate.minusDays(2), currentDate))
        .thenReturn(List.of(donation));

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.fill(center, currentDate, currentDate);

    assertEquals(1, result.get(request.getId()).fulfilledBloodBags());
  }

  @Test
  void fillRejectsInvertedDateWindow() {
    BloodCenter center = bloodCenter();

    assertThrows(
        IllegalArgumentException.class,
        () -> service.fill(center, currentDate, currentDate.minusDays(1), List.of(), List.of()));
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
