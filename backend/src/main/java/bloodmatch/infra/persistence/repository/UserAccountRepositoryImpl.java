package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import bloodmatch.infra.persistence.repository.mongo.UserAccountMongoRepository;
import bloodmatch.infra.persistence.schema.UserAccountSchema;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserAccountRepositoryImpl implements UserAccountRepositoryInterface {

  private final UserAccountMongoRepository mongoRepository;

  public UserAccountRepositoryImpl(UserAccountMongoRepository mongoRepository) {
    this.mongoRepository = mongoRepository;
  }

  @Override
  public Optional<UserAccount> findByEmail(Email email) {
    if (email == null)
      throw new IllegalArgumentException("Email cannot be null");

    return mongoRepository.findByEmail(email.getValue())
        .map(UserAccountSchema::toDomain);
  }

  @Override
  public Optional<UserAccount> findByPartyId(DomainID partyId) {
    if (partyId == null)
      throw new IllegalArgumentException("Party id cannot be null");

    return mongoRepository.findByPartyId(partyId.getValue().toString())
        .map(UserAccountSchema::toDomain);
  }

  @Override
  public void save(UserAccount userAccount) {
    if (userAccount == null)
      throw new IllegalArgumentException("UserAccount cannot be null");

    mongoRepository.save(new UserAccountSchema(userAccount));
  }
}
