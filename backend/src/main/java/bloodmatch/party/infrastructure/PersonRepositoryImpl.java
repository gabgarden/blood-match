package bloodmatch.party.infrastructure;
import bloodmatch.shared.infrastructure.persistence.repository.OptimisticConcurrency;

import bloodmatch.party.domain.Person;
import bloodmatch.party.domain.PersonRepositoryInterface;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.party.infrastructure.mongo.PartyMongoRepository;
import bloodmatch.party.infrastructure.schema.PartySchema;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PersonRepositoryImpl implements PersonRepositoryInterface {

  private final PartyMongoRepository mongoRepository;

  public PersonRepositoryImpl(PartyMongoRepository mongoRepository) {
    this.mongoRepository = mongoRepository;
  }

  @Override
  public Optional<Person> findById(DomainID personId) {
    if (personId == null)
      throw new IllegalArgumentException("Person id cannot be null");

    return mongoRepository.findByIdAndPartyType(personId.getValue().toString(), PartySchema.TYPE_PERSON)
        .map(PartySchema::toDomain)
        .map(Person.class::cast);
  }

  @Override
  public void save(Person person) {
    if (person == null)
      throw new IllegalArgumentException("Person cannot be null");

    OptimisticConcurrency.save(
        () -> mongoRepository.save(new PartySchema(person)),
        "Person");
  }
}
