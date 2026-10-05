package bloodmatch.role.domain;

import bloodmatch.party.domain.Organization;
import bloodmatch.party.domain.Person;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenter;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleHierarchyTest {

  @Test
  void testBloodCenterCreationAndReconstitution() {
    Organization org = new Organization("Hemocentro", new PhoneNumber("1133334444"), new CNPJ("12345678000100"));
    BloodCenter center = new BloodCenter(org);

    assertNotNull(center.getId());
    assertEquals(org, center.getOrganization());
    assertEquals(org, center.getParty());

    DomainID id = DomainID.generate();
    BloodCenter reconstituted1 = BloodCenter.reconstitute(org, id);
    assertEquals(id, reconstituted1.getId());

    BloodCenter reconstituted2 = BloodCenter.reconstitute(org, id, 5L);
    assertEquals(id, reconstituted2.getId());
    assertEquals(5L, reconstituted2.getVersion());

    assertThrows(IllegalArgumentException.class, () -> new BloodCenter(null));
  }

  @Test
  void testRequesterCreationAndReconstitution() {
    Person person = new Person("Pessoa", new PhoneNumber("11988887777"), new CPF("12345678901"), LocalDate.of(1990, 1, 1));
    Requester requester = new Requester(person);

    assertNotNull(requester.getId());
    assertEquals(person, requester.getParty());

    DomainID id = DomainID.generate();
    Requester reconstituted1 = Requester.reconstitute(person, id);
    assertEquals(id, reconstituted1.getId());

    Requester reconstituted2 = Requester.reconstitute(person, id, 3L);
    assertEquals(id, reconstituted2.getId());
    assertEquals(3L, reconstituted2.getVersion());

    assertThrows(IllegalArgumentException.class, () -> new Requester(null));
  }

  @Test
  void testPartyRoleValidation() {
    assertThrows(IllegalArgumentException.class, () -> new Requester(null));
    Person person = new Person("Pessoa", new PhoneNumber("11988887777"), new CPF("12345678901"), LocalDate.of(1990, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> Requester.reconstitute(person, null));
  }
}
