package bloodmatch.party.domain;

import bloodmatch.shared.domain.valueObjects.Address;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartyTest {

  @Test
  void testPersonDomainRules() {
    PhoneNumber phone = new PhoneNumber("11988887777");
    CPF cpf = new CPF("12345678901");
    LocalDate birth = LocalDate.of(2000, 1, 1);

    Person person = new Person("Joao Silva", phone, cpf, birth);

    assertEquals("Joao Silva", person.getName());
    assertEquals(phone, person.getPhoneNumber());
    assertEquals(cpf, person.getCpf());
    assertEquals(birth, person.getBirthDate());
    assertNull(person.getAddress());

    // Age
    assertEquals(26, person.getAge(LocalDate.of(2026, 6, 1)));
    assertThrows(IllegalArgumentException.class, () -> person.getAge(null));

    // Validations
    assertThrows(IllegalArgumentException.class, () -> new Person("Joao", phone, null, birth));
    assertThrows(IllegalArgumentException.class, () -> new Person("Joao", phone, cpf, null));
    assertThrows(IllegalArgumentException.class, () -> new Person("", phone, cpf, birth));
    assertThrows(IllegalArgumentException.class, () -> new Person(null, phone, cpf, birth));
    assertThrows(IllegalArgumentException.class, () -> new Person("Joao", null, cpf, birth));

    // Mutators
    person.changeName("Joao Santos");
    assertEquals("Joao Santos", person.getName());
    assertThrows(IllegalArgumentException.class, () -> person.changeName(""));
    assertThrows(IllegalArgumentException.class, () -> person.changeName(null));

    PhoneNumber newPhone = new PhoneNumber("11977776666");
    person.changePhoneNumber(newPhone);
    assertEquals(newPhone, person.getPhoneNumber());
    assertThrows(IllegalArgumentException.class, () -> person.changePhoneNumber(null));

    Address address = new Address("Rua 1", "Sao Paulo", "SP", "01001-000");
    person.changeAddress(address);
    assertEquals(address, person.getAddress());
  }

  @Test
  void testOrganizationDomainRules() {
    PhoneNumber phone = new PhoneNumber("1133334444");
    CNPJ cnpj = new CNPJ("12345678000100");

    Organization org = new Organization("Hospital Regional", phone, cnpj);
    assertEquals("Hospital Regional", org.getName());
    assertEquals(phone, org.getPhoneNumber());
    assertEquals(cnpj, org.getCnpj());

    assertThrows(IllegalArgumentException.class, () -> new Organization("Org", phone, null));
    assertThrows(IllegalArgumentException.class, () -> new Organization("", phone, cnpj));
  }
}
