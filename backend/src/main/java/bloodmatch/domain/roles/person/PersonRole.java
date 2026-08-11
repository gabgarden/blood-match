package bloodmatch.domain.roles.person;

import bloodmatch.domain.roles.PartyRole;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.shared.valueObjects.DomainID;

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