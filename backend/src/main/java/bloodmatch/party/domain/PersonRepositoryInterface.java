package bloodmatch.party.domain;

import bloodmatch.shared.domain.valueObjects.DomainID;

import java.util.Optional;

public interface PersonRepositoryInterface {

  Optional<Person> findById(DomainID personId);

  void save(Person person);
}
