package bloodmatch.role.domain.matching;

import bloodmatch.request.domain.DonationRequest;
import bloodmatch.request.domain.Urgency;
import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.person.donor.Donor;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.shared.domain.valueObjects.Address;
import bloodmatch.shared.domain.valueObjects.BloodType;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DonorMatchingServiceTest {

  private final LocalDate currentDate = LocalDate.of(2026, 4, 23);
  private final DonorMatchingService service = new DonorMatchingService();
  private final Address nearbyAddress = new Address("Street", "Sao Paulo", "SP", "01001000", -23.5505, -46.6333);

  @Test
  void shouldReturnOnlyCompatibleAndEligibleDonorsForAnActiveRequest() {
    Organization organization = new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100"));
    organization.changeAddress(nearbyAddress);

    DonationRequest request = DonationRequest.create(
      new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
      new BloodCenter(organization),
      BloodType.of("A+"), 1, currentDate.plusDays(2), currentDate, Urgency.MEDIUM, null);
    Donor compatible = donor("98765432100", BloodType.of("O-"));
    Donor incompatible = donor("12312312399", BloodType.of("B-"));
    Donor temporarilyIneligible = donor("32132132199", BloodType.of("O-"));
    temporarilyIneligible.registerDonation(currentDate.minusMonths(1), currentDate);

    List<Donor> result = service.findEligibleDonors(
        request, List.of(compatible, incompatible, temporarilyIneligible), currentDate);

    assertEquals(List.of(compatible), result);
  }

  @Test
  void testNullChecks() {
    Organization organization = new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100"));
    organization.changeAddress(nearbyAddress);

    DonationRequest request = DonationRequest.create(
      new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
      new BloodCenter(organization),
      BloodType.of("A+"), 1, currentDate.plusDays(2), currentDate, Urgency.MEDIUM, null);

    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> service.findEligibleDonors(null, List.of(), currentDate));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> service.findEligibleDonors(request, null, currentDate));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> service.findEligibleDonors(request, List.of(), null));
  }

  @Test
  void testDonorsWithoutAddressOrTooFar() {
    Organization organization = new Organization("Center", new PhoneNumber("1133334444"), new CNPJ("12345678000100"));
    organization.changeAddress(nearbyAddress);

    DonationRequest request = DonationRequest.create(
      new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
      new BloodCenter(organization),
      BloodType.of("A+"), 1, currentDate.plusDays(2), currentDate, Urgency.MEDIUM, null);

    // Donor with no address
    Person personNoAddr = new Person("DonorNoAddr", new PhoneNumber("11988887777"), new CPF("11122233344"), currentDate.minusYears(25));
    Donor donorNoAddr = new Donor(personNoAddr, BloodType.of("O-"), 70.0);

    // Donor very far away (e.g. Manaus, distance > max recommendation distance)
    Person personFar = new Person("DonorFar", new PhoneNumber("11988887777"), new CPF("55566677788"), currentDate.minusYears(25));
    personFar.changeAddress(new Address("Rua Manaus", "Manaus", "AM", "69000-000", -3.1, -60.0));
    Donor donorFar = new Donor(personFar, BloodType.of("O-"), 70.0);

    List<Donor> donors = new java.util.ArrayList<>();
    donors.add(null);
    donors.add(donorNoAddr);
    donors.add(donorFar);

    List<Donor> result = service.findEligibleDonors(request, donors, currentDate);
    assertEquals(0, result.size());

    // When blood center has no address
    Organization orgNoAddr = new Organization("CenterNoAddr", new PhoneNumber("1133334444"), new CNPJ("12345678000100"));
    DonationRequest reqNoAddr = DonationRequest.create(
      new Requester(new Person("Requester", new PhoneNumber("11999990000"), new CPF("12345678901"), LocalDate.of(1990, 1, 1))),
      new BloodCenter(orgNoAddr),
      BloodType.of("A+"), 1, currentDate.plusDays(2), currentDate, Urgency.MEDIUM, null);

    List<Donor> resultNoCenterAddr = service.findEligibleDonors(reqNoAddr, List.of(donor("98765432100", BloodType.of("O-"))), currentDate);
    assertEquals(0, resultNoCenterAddr.size());
  }

  private Donor donor(String cpf, BloodType type) {
    Person person = new Person("Donor", new PhoneNumber("11988887777"), new CPF(cpf), currentDate.minusYears(30));
    person.changeAddress(nearbyAddress);
    return new Donor(person, type, 70.0);
  }
}
