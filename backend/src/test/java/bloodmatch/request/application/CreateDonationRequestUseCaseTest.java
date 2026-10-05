package bloodmatch.request.application;

import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.request.application.CreateDonationRequestUseCase;
import bloodmatch.request.application.CreateDonationRequestUseCase.Input;
import bloodmatch.shared.domain.services.GeocodingServiceInterface;
import bloodmatch.party.domain.Person;
import bloodmatch.request.domain.DonationRequestRepositoryInterface;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.party.domain.PartyRepositoryInterface;
import bloodmatch.role.domain.requester.RequesterRepositoryInterface;
import bloodmatch.request.domain.DonationRequest;
import bloodmatch.party.domain.Organization;
import bloodmatch.shared.domain.valueObjects.Address;
import bloodmatch.role.domain.requester.Requester;
import bloodmatch.shared.domain.valueObjects.CNPJ;
import bloodmatch.shared.domain.valueObjects.CPF;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.shared.domain.valueObjects.PhoneNumber;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateDonationRequestUseCaseTest {

  private final DonationRequestRepositoryInterface donationRequestRepository = mock(
      DonationRequestRepositoryInterface.class);
  private final RequesterRepositoryInterface requesterRepository = mock(RequesterRepositoryInterface.class);
  private final BloodCenterRepositoryInterface bloodCenterRepository = mock(BloodCenterRepositoryInterface.class);
  private final PartyRepositoryInterface partyRepository = mock(PartyRepositoryInterface.class);

  private final GeocodingServiceInterface geocodingService = mock(GeocodingServiceInterface.class);

  private final CreateDonationRequestUseCase useCase = new CreateDonationRequestUseCase(donationRequestRepository,
      requesterRepository,
      bloodCenterRepository,
      partyRepository,
      geocodingService);

  @Test
  void shouldCreateAndSaveDonationRequest() {
    LocalDate currentDate = LocalDate.of(2026, 3, 16);
    LocalDate dateLimit = currentDate.plusDays(10);

    Person requesterParty = new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1));
    Requester requester = new Requester(requesterParty);

    Organization bloodCenterParty = new Organization(
        "Main Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100"));
    DomainID requesterId = DomainID.generate();
    DomainID bloodCenterId = DomainID.generate();

    when(requesterRepository.findByPartyId(requesterId)).thenReturn(Optional.of(requester));
    when(bloodCenterRepository.findByPartyId(bloodCenterId)).thenReturn(
        Optional.of(new bloodmatch.role.domain.organization.bloodcenter.BloodCenter(bloodCenterParty)));

    CreateDonationRequestUseCase.Output output = useCase.execute(
        new Input(
            requesterId.getValue().toString(),
            bloodCenterId.getValue().toString(),
            "A+",
            3,
            dateLimit,
            "MEDIUM",
            null),
        currentDate);

    assertNotNull(output);
    assertNotNull(output.id());
    verify(donationRequestRepository).save(any(DonationRequest.class));
  }

  @Test
  void shouldGeocodeBloodCenterAddressWhenMissingCoordinates() {
    LocalDate currentDate = LocalDate.of(2026, 3, 16);
    LocalDate dateLimit = currentDate.plusDays(10);

    Person requesterParty = new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1));
    Requester requester = new Requester(requesterParty);

    Organization bloodCenterParty = new Organization(
        "Main Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100"));
    bloodCenterParty.changeAddress(new Address(
        "Rua A", "São Paulo", "SP", "01001000"));

    Address geocodedAddress = new Address(
        "Rua A", "São Paulo", "SP", "01001000", -23.55, -46.6);

    DomainID requesterId = DomainID.generate();
    DomainID bloodCenterId = DomainID.generate();

    when(requesterRepository.findByPartyId(requesterId)).thenReturn(Optional.of(requester));
    when(bloodCenterRepository.findByPartyId(bloodCenterId)).thenReturn(
        Optional.of(new bloodmatch.role.domain.organization.bloodcenter.BloodCenter(bloodCenterParty)));
    when(geocodingService.getCoordinatesFromAddress(any())).thenReturn(geocodedAddress);

    CreateDonationRequestUseCase.Output output = useCase.execute(
        new Input(
            requesterId.getValue().toString(),
            bloodCenterId.getValue().toString(),
            "A+",
            3,
            dateLimit,
            "MEDIUM",
            null),
        currentDate);

    assertNotNull(output);
    verify(geocodingService).getCoordinatesFromAddress(any());
    verify(partyRepository).save(bloodCenterParty);
    verify(donationRequestRepository).save(any(DonationRequest.class));
  }

  @Test
  void shouldThrowWhenRequesterRoleIsMissing() {
    LocalDate currentDate = LocalDate.of(2026, 3, 16);
    DomainID requesterId = DomainID.generate();
    DomainID bloodCenterId = DomainID.generate();

    Organization bloodCenterParty = new Organization(
        "Main Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100"));
    when(requesterRepository.findByPartyId(requesterId)).thenReturn(Optional.empty());
    when(bloodCenterRepository.findByPartyId(bloodCenterId)).thenReturn(
        Optional.of(new bloodmatch.role.domain.organization.bloodcenter.BloodCenter(bloodCenterParty)));

    assertThrows(
        NotFoundException.class,
        () -> useCase.execute(
            new Input(
                requesterId.getValue().toString(),
                bloodCenterId.getValue().toString(),
                "A+",
                3,
                currentDate.plusDays(10),
                "MEDIUM",
                null),
            currentDate));
  }

  @Test
  void shouldRejectNullConstructorArgs() {
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationRequestUseCase(null, requesterRepository, bloodCenterRepository, partyRepository, geocodingService));
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationRequestUseCase(donationRequestRepository, null, bloodCenterRepository, partyRepository, geocodingService));
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationRequestUseCase(donationRequestRepository, requesterRepository, null, partyRepository, geocodingService));
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationRequestUseCase(donationRequestRepository, requesterRepository, bloodCenterRepository, null, geocodingService));
    assertThrows(IllegalArgumentException.class, () -> new CreateDonationRequestUseCase(donationRequestRepository, requesterRepository, bloodCenterRepository, partyRepository, null));
  }

  @Test
  void shouldValidateInputFields() {
    LocalDate today = LocalDate.now();
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(null));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(new Input("id", "id", "A+", 3, today.plusDays(1), "MEDIUM", null), null));

    String pId = DomainID.generate().getValue().toString();
    String oId = DomainID.generate().getValue().toString();

    // Goal bags null or invalid
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, "A+", null, today.plusDays(1), "MEDIUM", null), today));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, "A+", 0, today.plusDays(1), "MEDIUM", null), today));

    // Date limit null or in past
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, "A+", 5, null, "MEDIUM", null), today));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, "A+", 5, today.minusDays(1), "MEDIUM", null), today));

    // Blood type null, blank or invalid
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, null, 5, today.plusDays(1), "MEDIUM", null), today));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, "", 5, today.plusDays(1), "MEDIUM", null), today));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, "INVALID", 5, today.plusDays(1), "MEDIUM", null), today));

    // Urgency null, blank or invalid
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, "A+", 5, today.plusDays(1), null, null), today));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, "A+", 5, today.plusDays(1), "", null), today));
    assertThrows(bloodmatch.shared.application.exception.ValidationException.class, () -> useCase.execute(
        new Input(pId, oId, "A+", 5, today.plusDays(1), "SUPER_URGENT", null), today));
  }

  @Test
  void shouldThrowWhenBloodCenterRoleIsMissing() {
    LocalDate currentDate = LocalDate.of(2026, 3, 16);
    DomainID requesterId = DomainID.generate();
    DomainID bloodCenterId = DomainID.generate();

    Person requesterParty = new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1));
    Requester requester = new Requester(requesterParty);

    when(requesterRepository.findByPartyId(requesterId)).thenReturn(Optional.of(requester));
    when(bloodCenterRepository.findByPartyId(bloodCenterId)).thenReturn(Optional.empty());

    assertThrows(
        NotFoundException.class,
        () -> useCase.execute(
            new Input(
                requesterId.getValue().toString(),
                bloodCenterId.getValue().toString(),
                "A+",
                3,
                currentDate.plusDays(10),
                "MEDIUM",
                null),
            currentDate));
  }

  @Test
  void shouldExecuteWithDefaultCurrentDate() {
    Person requesterParty = new Person(
        "Requester Person",
        new PhoneNumber("11999990000"),
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1));
    Requester requester = new Requester(requesterParty);

    Organization bloodCenterParty = new Organization(
        "Main Blood Center",
        new PhoneNumber("1133334444"),
        new CNPJ("12345678000100"));
    DomainID requesterId = DomainID.generate();
    DomainID bloodCenterId = DomainID.generate();

    when(requesterRepository.findByPartyId(requesterId)).thenReturn(Optional.of(requester));
    when(bloodCenterRepository.findByPartyId(bloodCenterId)).thenReturn(
        Optional.of(new bloodmatch.role.domain.organization.bloodcenter.BloodCenter(bloodCenterParty)));

    var output = useCase.execute(
        new Input(
            requesterId.getValue().toString(),
            bloodCenterId.getValue().toString(),
            "A+",
            3,
            LocalDate.now().plusDays(10),
            "MEDIUM",
            null));

    assertNotNull(output);
  }
}
