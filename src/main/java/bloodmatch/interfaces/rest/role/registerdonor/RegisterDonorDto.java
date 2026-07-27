package bloodmatch.interfaces.rest.role.registerdonor;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to register a person as a donor.")
public record RegisterDonorDto(
    @Schema(description = "UUID of the person being registered as a donor.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String personId,
    @Schema(description = "Donor blood type.", example = "O+", allowableValues = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}, requiredMode = Schema.RequiredMode.REQUIRED) String bloodType,
    @Schema(description = "Donor weight in kilograms.", example = "72.5", minimum = "0.1", requiredMode = Schema.RequiredMode.REQUIRED) Double weight) {
}
