package bloodmatch.application.usecase;

import bloodmatch.application.usecase.donationrequest.CreateDonationRequestUseCase;
import bloodmatch.domain.services.GeocodingServiceInterface;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.PartyRepositoryInterface;
import bloodmatch.domain.repositories.RequesterRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.Urgency;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.domain.shared.valueObjects.BloodType;
import bloodmatch.domain.shared.valueObjects.CNPJ;
import bloodmatch.domain.shared.valueObjects.CPF;
import bloodmatch.domain.shared.valueObjects.DomainID;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
  private final PartyRepositoryInterface partyRepository = mock(PartyRepositoryInterface.class);

  private final GeocodingServiceInterface geocodingService = mock(GeocodingServiceInterface.class);

  private final CreateDonationRequestUseCase useCase = new CreateDonationRequestUseCase(donationRequestRepository,
      requesterRepository,
      partyRepository,
      geocodingService);

  @Test
  void shouldCreateAndSaveDonationRequest() {
    LocalDate currentDate = LocalDate.of(2026, 3, 16);
    LocalDate dateLimit = currentDate.plusDays(10);

    Person requesterParty = new Person(
        "Requester Person",
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1));
    Requester requester = new Requester(requesterParty);

    Organization bloodCenterParty = new Organization(
        "Main Blood Center",
        new CNPJ("12345678000100"));
    DomainID requesterId = DomainID.generate();
    DomainID bloodCenterId = DomainID.generate();

    when(requesterRepository.findByPartyId(requesterId)).thenReturn(Optional.of(requester));
    when(partyRepository.findById(bloodCenterId)).thenReturn(Optional.of(bloodCenterParty));

    DonationRequest request = useCase.execute(
        requesterId,
        bloodCenterId,
        BloodType.of("A+"),
        dateLimit,
        currentDate,
        Urgency.MEDIUM);

    assertNotNull(request);
    assertEquals(currentDate, request.getDateRequested());
    assertEquals(dateLimit, request.getDateLimit());
    verify(donationRequestRepository).save(any(DonationRequest.class));
  }

  @Test
  void shouldGeocodeBloodCenterAddressWhenMissingCoordinates() {
    LocalDate currentDate = LocalDate.of(2026, 3, 16);
    LocalDate dateLimit = currentDate.plusDays(10);

    Person requesterParty = new Person(
        "Requester Person",
        new CPF("12345678901"),
        LocalDate.of(1995, 1, 1));
    Requester requester = new Requester(requesterParty);

    Organization bloodCenterParty = new Organization(
        "Main Blood Center",
        new CNPJ("12345678000100"));
    bloodCenterParty.changeAddress(new Address(
        "Rua A", "São Paulo", "SP", "01000-000"));

    Address geocodedAddress = new Address(
        "Rua A", "São Paulo", "SP", "01000-000", -23.55, -46.63);

    DomainID requesterId = DomainID.generate();
    DomainID bloodCenterId = DomainID.generate();

    when(requesterRepository.findByPartyId(requesterId)).thenReturn(Optional.of(requester));
    when(partyRepository.findById(bloodCenterId)).thenReturn(Optional.of(bloodCenterParty));
    when(geocodingService.getCoordinatesFromAddress(any())).thenReturn(geocodedAddress);

    DonationRequest request = useCase.execute(
        requesterId,
        bloodCenterId,
        BloodType.of("A+"),
        dateLimit,
        currentDate,
        Urgency.MEDIUM);

    assertNotNull(request);
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
        new CNPJ("12345678000100"));
    when(requesterRepository.findByPartyId(requesterId)).thenReturn(Optional.empty());
    when(partyRepository.findById(bloodCenterId)).thenReturn(Optional.of(bloodCenterParty));

    assertThrows(
        IllegalArgumentException.class,
        () -> useCase.execute(
            requesterId,
            bloodCenterId,
            BloodType.of("A+"),
            currentDate.plusDays(10),
                        currentDate,
                        Urgency.MEDIUM));
  }
}