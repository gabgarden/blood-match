package bloodmatch.application.usecase.party;

import bloodmatch.application.exception.ConcurrencyException;
import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.domain.party.Party;
import bloodmatch.domain.party.PartyRepositoryInterface;
import bloodmatch.domain.services.GeocodingServiceInterface;
import bloodmatch.domain.shared.valueObjects.Address;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.PhoneNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class UpdatePartyUseCase {

  private final PartyRepositoryInterface partyRepository;
  private final GeocodingServiceInterface geocodingService;

  public UpdatePartyUseCase(PartyRepositoryInterface partyRepository, GeocodingServiceInterface geocodingService) {
    if (partyRepository == null)
      throw new IllegalArgumentException("PartyRepository cannot be null");
    if (geocodingService == null)
      throw new IllegalArgumentException("GeocodingService cannot be null");

    this.partyRepository = partyRepository;
    this.geocodingService = geocodingService;
  }

  @Transactional
  public Output execute(Input input) {
    if (input == null)
      throw new ValidationException("Request body cannot be null");
    if (input.version() == null)
      throw new ValidationException("version cannot be null");

    if ((input.name() == null || input.name().isBlank()) && input.phoneNumber() == null && input.address() == null) {
      throw new ValidationException("At least one mutable field must be provided");
    }

    DomainID partyId = DomainIdParser.parse(input.partyId(), "partyId");

    Party party = partyRepository.findById(partyId)
        .orElseThrow(() -> new NotFoundException("Party not found"));

    if (!Objects.equals(party.getVersion(), input.version())) {
      throw new ConcurrencyException("Resource version conflict: expected " + input.version() + " but found " + party.getVersion());
    }

    if (input.name() != null && !input.name().isBlank()) {
      party.changeName(input.name());
    }

    if (input.phoneNumber() != null) {
      party.changePhoneNumber(new PhoneNumber(input.phoneNumber()));
    }

    if (input.address() != null) {
      Address newAddress = new Address(
          input.address().street(),
          input.address().city(),
          input.address().state(),
          input.address().zipCode());
      
      Address geocodedAddress = geocodingService.getCoordinatesFromAddress(newAddress);
      party.changeAddress(geocodedAddress);
    }

    partyRepository.save(party);
    return Output.from(party);
  }

  public record Input(String partyId, Long version, String name, String phoneNumber, AddressInput address) {
  }

  public record AddressInput(String street, String city, String state, String zipCode) {
  }

  public record Output(String id, Long version, String name, String phoneNumber, AddressOutput address) {
    public static Output from(Party party) {
      AddressOutput addressOutput = null;
      if (party.getAddress() != null) {
        addressOutput = new AddressOutput(
            party.getAddress().getStreet(),
            party.getAddress().getCity(),
            party.getAddress().getState(),
            party.getAddress().getZipCode());
      }
      return new Output(
          party.getId().getValue().toString(),
          party.getVersion(),
          party.getName(),
          party.getPhoneNumber() != null ? party.getPhoneNumber().getValue() : null,
          addressOutput);
    }
  }

  public record AddressOutput(String street, String city, String state, String zipCode) {
  }
}
