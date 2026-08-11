package bloodmatch.interfaces.rest.donationrequest.getdonationrequestsbypartyid;

import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase.OutputItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Donation request associated with a party.")
public record DonationRequestByPartyResponseDto(
    @Schema(description = "Request UUID") String requestId,
    @Schema(description = "Blood type needed") String bloodTypeNeeded,
    @Schema(description = "Date requested", format = "date") LocalDate dateRequested,
    @Schema(description = "Date limit", format = "date") LocalDate dateLimit,
    @Schema(description = "Whether the request is active") boolean active,
    @Schema(description = "Whether the request is expired") boolean expired,
    @Schema(description = "Blood center name") String bloodCenterName,
    @Schema(description = "Blood center phone number") String bloodCenterPhoneNumber,
    @Schema(description = "Urgency level") String urgency,
    @Schema(description = "Goal blood bags") int goalBloodBags,
    @Schema(description = "Fulfilled blood bags") int fulfilledBloodBags,
    @Schema(description = "Remaining blood bags") int remainingBloodBags,
    @Schema(description = "Whether goal was reached") boolean goalReached) {

  public static DonationRequestByPartyResponseDto from(OutputItem item) {
    return new DonationRequestByPartyResponseDto(
        item.requestId(),
        item.bloodTypeNeeded(),
        item.dateRequested(),
        item.dateLimit(),
        item.active(),
        item.expired(),
        item.bloodCenterName(),
        item.bloodCenterPhoneNumber(),
        item.urgency(),
        item.goalBloodBags(),
        item.fulfilledBloodBags(),
        item.remainingBloodBags(),
        item.goalReached());
  }
}
