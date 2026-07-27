package bloodmatch.interfaces.rest.donation.gethistory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Donor identifier used to retrieve donation history.")
public record GetDonorDonationHistoryBodyDto(
    @Schema(description = "UUID of the donor.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED) String donorId) {
}
