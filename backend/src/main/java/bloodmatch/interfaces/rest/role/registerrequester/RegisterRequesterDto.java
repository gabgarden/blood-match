package bloodmatch.interfaces.rest.role.registerrequester;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to register a party as a donation requester.")
public record RegisterRequesterDto(
    @Schema(description = "UUID of the party being registered as a requester.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String partyId) {
}
