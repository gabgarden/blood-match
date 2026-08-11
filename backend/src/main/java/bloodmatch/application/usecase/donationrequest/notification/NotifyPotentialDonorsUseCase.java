package bloodmatch.application.usecase.donationrequest.notification;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.shared.PartyOwnership;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.matching.DonorMatchingService;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.services.NotificationServiceInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class NotifyPotentialDonorsUseCase {

  private final DonationRequestRepositoryInterface requestRepository;
  private final DonorRepositoryInterface donorRepository;
  private final UserAccountRepositoryInterface userAccountRepository;
  private final DonorMatchingService matchingService;
  private final NotificationServiceInterface notificationService;

  public NotifyPotentialDonorsUseCase(
      DonationRequestRepositoryInterface requestRepository,
      DonorRepositoryInterface donorRepository,
      UserAccountRepositoryInterface userAccountRepository,
      NotificationServiceInterface notificationService) {

    this.requestRepository = requestRepository;
    this.donorRepository = donorRepository;
    this.userAccountRepository = userAccountRepository;
    this.notificationService = notificationService;

    this.matchingService = new DonorMatchingService();
  }

  public Output execute(Input input) {
    return execute(input, LocalDate.now());
  }

  public Output execute(Input input, LocalDate currentDate) {
    if (input == null) {
      throw new ValidationException("Input cannot be null");
    }
    if (currentDate == null) {
      throw new ValidationException("Current date cannot be null");
    }

    DomainID requestId = DomainIdParser.parse(input.requestId(), "requestId");

    DonationRequest request = requestRepository.findById(requestId)
        .orElseThrow(() -> new NotFoundException("Donation request not found"));

    PartyOwnership.requireSameParty(request.getRequester().getParty().getId(), input.actorPartyId());

    if (currentDate.isAfter(request.getDateLimit())) {
      throw new ValidationException(
          "Cannot notify donors. The donation request has already expired.");
    }

    if (request.isGoalReached()) {
      throw new ValidationException(
          "Cannot notify donors. The goal for this request has already been reached.");
    }

    List<Donor> allDonors = donorRepository.findAll();
    List<Donor> eligibleDonors = matchingService.findEligibleDonors(request, allDonors, currentDate);

    if (eligibleDonors.isEmpty()) {
      System.out.println("Nenhum doador elegível encontrado para a requisição: " + requestId.getValue());
      return Output.success();
    }

    for (Donor donor : eligibleDonors) {
      DomainID partyId = donor.getPerson().getId();

      UserAccount account = userAccountRepository.findByPartyId(partyId)
          .orElse(null);

      if (account != null && account.isEnabled()) {
        notificationService.notifyDonorAboutRequest(donor, account, request);
      } else {
        System.out.println("Conta inativa ou não encontrada para o doador: " + donor.getPerson().getName());
      }
    }

    return Output.success();
  }

  public record Input(String requestId, String actorPartyId) {
  }

  public record Output(String message) {
    public static Output success() {
      return new Output("Notifications sent to eligible donors successfully.");
    }
  }
}
