package bloodmatch.auth.infrastructure;
import bloodmatch.shared.infrastructure.persistence.repository.OptimisticConcurrency;

import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.Email;
import bloodmatch.auth.infrastructure.mongo.UserAccountMongoRepository;
import bloodmatch.auth.infrastructure.schema.UserAccountSchema;
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
  public Optional<UserAccount> findByEmailConfirmationToken(String token) {
    if (token == null || token.isBlank())
      return Optional.empty();

    return mongoRepository.findByEmailConfirmationToken(token)
        .map(UserAccountSchema::toDomain);
  }

  @Override
  public void save(UserAccount userAccount) {
    if (userAccount == null)
      throw new IllegalArgumentException("UserAccount cannot be null");

    OptimisticConcurrency.save(
        () -> mongoRepository.save(new UserAccountSchema(userAccount)),
        "User account");
  }
}
