package bloodmatch.interfaces.rest.party.registerperson;

import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to register a person.")
public record RegisterPersonDto(
    @Schema(description = "Person full name.", example = "Ana Silva", requiredMode = Schema.RequiredMode.REQUIRED) String name,
    @Schema(description = "Brazilian individual taxpayer registry number.", example = "12345678909", requiredMode = Schema.RequiredMode.REQUIRED) String cpf,
    @Schema(description = "Person date of birth.", format = "date", example = "1990-05-15", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate birthDate,
    @Schema(description = "Account email address.", format = "email", example = "ana.silva@example.com", requiredMode = Schema.RequiredMode.REQUIRED) String email,
    @Schema(description = "Account password.", format = "password", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED) String password,
    @Schema(description = "Confirmation of the account password.", format = "password", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED) String passwordConfirmation,
    @Schema(description = "Street address. Required when an address is provided.", example = "Main Street, 123") String street,
    @Schema(description = "City. Required when an address is provided.", example = "Sao Paulo") String city,
    @Schema(description = "State or region. Required when an address is provided.", example = "SP") String state,
    @Schema(description = "Postal code. Required when an address is provided.", example = "01001000") String zipCode) {
}
