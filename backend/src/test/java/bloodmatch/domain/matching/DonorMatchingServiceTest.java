package bloodmatch.domain.matching;

import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
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
