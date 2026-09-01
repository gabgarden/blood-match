package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.roles.person.donor.DonorRepositoryInterface;
import bloodmatch.domain.party.PersonRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.DonorMongoRepository;
import bloodmatch.infra.persistence.schema.DonorSchema;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DonorRepositoryImpl implements DonorRepositoryInterface {

  private final DonorMongoRepository mongoRepository;
  private final PersonRepositoryInterface personRepository;

  public DonorRepositoryImpl(
      DonorMongoRepository mongoRepository,
      PersonRepositoryInterface personRepository) {
    this.mongoRepository = mongoRepository;
    this.personRepository = personRepository;
  }

  @Override
  public Optional<Donor> findByPartyId(DomainID personId) {
    if (personId == null)
      throw new IllegalArgumentException("Person id cannot be null");

    return mongoRepository.findByPersonId(personId.getValue().toString())
        .map(schema -> schema.toDomain(personRepository));
  }

  @Override
  public List<Donor> findAll() {
    List<DonorSchema> schemas = mongoRepository.findAll();

    return schemas.stream()
        .map(schema -> schema.toDomain(personRepository))
        .toList();
  }

  @Override
  public void save(Donor donor) {
    if (donor == null)
      throw new IllegalArgumentException("Donor cannot be null");

    OptimisticConcurrency.save(
        () -> mongoRepository.save(new DonorSchema(donor)),
        "Donor");
  }
}