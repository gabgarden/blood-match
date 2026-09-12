package bloodmatch.interfaces.rest.party.updateparty;

public record UpdatePartyDto(
    String partyId,
    Long version,
    String name,
    String phoneNumber,
    AddressDto address) {

  public record AddressDto(
      String street,
      String city,
      String state,
      String zipCode) {
  }
}
