package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.PersonRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.PartyMongoRepository;
import bloodmatch.infra.persistence.schema.PartySchema;
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

    mongoRepository.save(new PartySchema(person));
  }
}
