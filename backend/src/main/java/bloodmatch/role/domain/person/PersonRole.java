package bloodmatch.role.domain.person;

import bloodmatch.role.domain.PartyRole;
import bloodmatch.party.domain.Person;
import bloodmatch.shared.domain.valueObjects.DomainID;

public abstract class PersonRole extends PartyRole<Person> {

  protected PersonRole(Person person) {
    super(person);
  }

  protected PersonRole(Person person, DomainID id) {
    super(person, id);
  }

  public Person getPerson() {
    return getParty();
  }

}