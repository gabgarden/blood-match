package bloodmatch.domain.party;

import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.Optional;

public interface PersonRepositoryInterface {

  Optional<Person> findById(DomainID personId);

  void save(Person person);
}
