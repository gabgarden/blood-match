package bloodmatch.interfaces.rest.party.updateparty;

import bloodmatch.application.usecase.party.UpdatePartyUseCase;

public record UpdatePartyResponseDto(
    String id,
    Long version,
    String name,
    String phoneNumber,
    AddressResponseDto address) {

  public static UpdatePartyResponseDto from(UpdatePartyUseCase.Output output) {
    AddressResponseDto addressDto = null;
    if (output.address() != null) {
      addressDto = new AddressResponseDto(
          output.address().street(),
          output.address().city(),
          output.address().state(),
          output.address().zipCode());
    }

    return new UpdatePartyResponseDto(
        output.id(),
        output.version(),
        output.name(),
        output.phoneNumber(),
        addressDto);
  }

  public record AddressResponseDto(
      String street,
      String city,
      String state,
      String zipCode) {
  }
}
