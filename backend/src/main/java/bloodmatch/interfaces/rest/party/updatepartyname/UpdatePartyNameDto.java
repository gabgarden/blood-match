package bloodmatch.interfaces.rest.party.updatepartyname;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Data required to update a party name.")
public record UpdatePartyNameDto(
    @Schema(description = "UUID of the party.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String partyId,
    @Schema(description = "New name of the person or organization.", example = "St. Luke Hospital", requiredMode = Schema.RequiredMode.REQUIRED) String newName) {
}
