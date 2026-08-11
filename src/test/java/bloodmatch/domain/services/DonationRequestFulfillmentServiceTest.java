package bloodmatch.domain.services;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Foco em synchronize(): materializa fulfilledBloodBags e recalcula do zero (não incrementa).
 */
class DonationRequestFulfillmentServiceTest {

  private final LocalDate currentDate = LocalDate.of(2026, 4, 23);
  private final DonationRequestFulfillmentService service = new DonationRequestFulfillmentService();

  @Test
  void synchronizePersistsCalculatedCountersOntoRequestEntities() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 1, 0);
    Donation donation = donation(1, center, currentDate);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result =
        service.synchronize(List.of(request), List.of(donation), currentDate);

    assertEquals(1, request.getFulfilledBloodBags());
    assertTrue(request.isGoalReached());
    assertEquals(1, result.get(request.getId()).fulfilledBloodBags());
    assertTrue(result.get(request.getId()).goalReached());
  }

  @Test
  void synchronizeRecalculatesFromScratchInsteadOfIncrementing() {
    BloodCenter center = bloodCenter();
    // contador stale alto (como se viesse do banco desatualizado)
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 2, 99);
    Donation onlyOne = donation(1, center, currentDate);

    service.synchronize(List.of(request), List.of(onlyOne), currentDate);

    assertEquals(1, request.getFulfilledBloodBags());
    assertFalse(request.isGoalReached());
  }

  @Test
  void synchronizeResetsCountersToZeroWhenPoolNoLongerAllocates() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 1, 1);
    // doação fora da janela → alocação zero
    Donation outsideWindow = donation(1, center, currentDate.minusDays(10));

    service.synchronize(List.of(request), List.of(outsideWindow), currentDate);

    assertEquals(0, request.getFulfilledBloodBags());
    assertFalse(request.isGoalReached());
  }

  @Test
  void synchronizeDistributesAcrossMultipleRequestsAndMaterializesAll() {
    BloodCenter center = bloodCenter();
    DonationRequest oldest = request(1, center, currentDate.minusDays(3), currentDate.plusDays(5), 1, 0);
    DonationRequest newest = request(2, center, currentDate.minusDays(1), currentDate.plusDays(5), 1, 0);
    List<Donation> donations = List.of(
        donation(1, center, currentDate.minusDays(1)),
        donation(2, center, currentDate));

    service.synchronize(List.of(newest, oldest), donations, currentDate);

    assertEquals(1, oldest.getFulfilledBloodBags());
    assertEquals(1, newest.getFulfilledBloodBags());
    assertTrue(oldest.isGoalReached());
    assertTrue(newest.isGoalReached());
  }

  @Test
  void synchronizeLeavesAllAtZeroWhenDonationsListIsEmpty() {
    BloodCenter center = bloodCenter();
    DonationRequest request = request(1, center, currentDate.minusDays(2), currentDate.plusDays(5), 1, 3);

    service.synchronize(List.of(request), List.of(), currentDate);

    assertEquals(0, request.getFulfilledBloodBags());
  }

  private DonationRequest request(
      long id,
      BloodCenter center,
      LocalDate requestedAt,
      LocalDate limit,
      int goal,
      int previousFulfilled) {
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
        previousFulfilled,
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
