package bloodmatch.domain.repositories;

import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;

import java.util.Optional;

public interface UserAccountRepositoryInterface {

  Optional<UserAccount> findByEmail(Email email);

  Optional<UserAccount> findByPartyId(DomainID partyId);

  void save(UserAccount userAccount);
}
