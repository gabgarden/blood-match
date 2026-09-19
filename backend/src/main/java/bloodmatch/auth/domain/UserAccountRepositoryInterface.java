package bloodmatch.auth.domain;

import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.Email;

import java.util.Optional;

public interface UserAccountRepositoryInterface {

  Optional<UserAccount> findByEmail(Email email);

  Optional<UserAccount> findByPartyId(DomainID partyId);

  Optional<UserAccount> findByEmailConfirmationToken(String token);

  void save(UserAccount userAccount);
}
