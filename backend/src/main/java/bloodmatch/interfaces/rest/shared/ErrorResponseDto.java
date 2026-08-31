package bloodmatch.interfaces.rest.shared;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API error response")
public record ErrorResponseDto(
    @Schema(description = "Error message", example = "personId cannot be blank")
    String error) {
}
