package bloodmatch.interfaces.rest.party.registerorganization;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to register an organization.")
public record RegisterOrganizationDto(
    @Schema(description = "Organization legal name.", example = "St. Luke Hospital", requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(description = "Brazilian corporate taxpayer registry number.", example = "11222333000181", requiredMode = Schema.RequiredMode.REQUIRED) String cnpj,
    @Schema(description = "Organization account email address.", format = "email", example = "contact@stluke.example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email,
    @Schema(description = "Account password.", format = "password", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED) String password,
    @Schema(description = "Confirmation of the account password.", format = "password", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED) String passwordConfirmation,
    @Schema(description = "Street address. Required when an address is provided.", example = "Main Street, 123") String street,
    @Schema(description = "City. Required when an address is provided.", example = "Sao Paulo") String city,
    @Schema(description = "State or region. Required when an address is provided.", example = "SP") String state,
    @Schema(description = "Postal code. Required when an address is provided.", example = "01001000") String zipCode) {
}
