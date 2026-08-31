package bloodmatch.interfaces.rest.role.updatedonorprofile;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to update a donor profile.")
public record UpdateDonorProfileDto(
    @Schema(description = "UUID of the donor.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String personId,
    @Schema(description = "Updated donor blood type.", example = "O+", allowableValues = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}, requiredMode = Schema.RequiredMode.REQUIRED) String bloodType,
    @Schema(description = "Updated donor weight in kilograms.", example = "72.5", minimum = "0.1", requiredMode = Schema.RequiredMode.REQUIRED) Double weight) {
}
