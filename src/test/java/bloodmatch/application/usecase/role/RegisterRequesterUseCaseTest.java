package bloodmatch.application.usecase.role;

import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.repositories.RequesterRepositoryInterface;
import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterRequesterUseCaseTest {

  private final RequesterRepositoryInterface requesterRepository = mock(RequesterRepositoryInterface.class);
  private final PartyRepositoryInterface partyRepository = mock(PartyRepositoryInterface.class);
  private final UserAccountRepositoryInterface userAccountRepository = mock(UserAccountRepositoryInterface.class);

  private final RegisterRequesterUseCase useCase = new RegisterRequesterUseCase(
      requesterRepository,
      partyRepository,
      userAccountRepository);

  @Test
  void shouldAddRequesterRoleToUserAccountWhenRegisteringRequester() {
    Person party = new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("98765432100"),
        LocalDate.of(1992, 2, 2));
    DomainID partyId = party.getId();

    UserAccount userAccount = new UserAccount(
        partyId,
        new Email("requester@bloodmatch.com"),
        "hash",
        Set.of());

    when(partyRepository.findById(partyId)).thenReturn(Optional.of(party));
    when(requesterRepository.findByPartyId(partyId)).thenReturn(Optional.empty());
    when(userAccountRepository.findByPartyId(partyId)).thenReturn(Optional.of(userAccount));

    useCase.execute(partyId);

    assertTrue(userAccount.getRoles().contains(SecurityRole.REQUESTER));
    verify(requesterRepository).save(any());
    verify(userAccountRepository).save(userAccount);
  }
}
