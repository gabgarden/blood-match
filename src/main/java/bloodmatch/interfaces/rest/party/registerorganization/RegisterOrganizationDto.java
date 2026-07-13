package bloodmatch.interfaces.rest.party.registerorganization;

public record RegisterOrganizationDto(
    String name,
    String cnpj,
    String email,
    String password,
    String passwordConfirmation,
    String street,
    String number,
    String neighborhood,
    String city,
    String state,
    String zipCode) {
}
