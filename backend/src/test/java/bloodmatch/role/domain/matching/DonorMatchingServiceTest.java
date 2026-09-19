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

  private Donor donor(String cpf, BloodType type) {
    Person person = new Person("Donor", new PhoneNumber("11988887777"), new CPF(cpf), currentDate.minusYears(30));
    person.changeAddress(nearbyAddress);
    return new Donor(person, type, 70.0);
  }
}
