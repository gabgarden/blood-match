package bloodmatch.role.application;

import bloodmatch.role.application.RegisterRequesterUseCase.Input;
import bloodmatch.role.application.RegisterRequesterUseCase.Output;
import bloodmatch.party.domain.Person;
import bloodmatch.party.domain.PartyRepositoryInterface;
import bloodmatch.role.domain.requester.RequesterRepositoryInterface;
import bloodmatch.auth.domain.UserAccountRepositoryInterface;
import bloodmatch.auth.domain.SecurityRole;
import bloodmatch.auth.domain.UserAccount;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.Email;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    Output output = useCase.execute(new Input(partyId.getValue().toString()));

    assertNotNull(output.id());
    assertTrue(userAccount.getRoles().contains(SecurityRole.REQUESTER));
    verify(requesterRepository).save(any());
    verify(userAccountRepository).save(userAccount);
  }
}
