package bloodmatch.interfaces.rest.role.registerbloodcenter;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to register an organization as a blood center.")
public record RegisterBloodCenterDto(
    @Schema(description = "UUID of the organization.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String organizationId
) {
}
