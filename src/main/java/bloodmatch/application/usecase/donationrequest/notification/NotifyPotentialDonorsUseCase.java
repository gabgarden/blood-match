package bloodmatch.application.usecase.donationrequest.notification;

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

    public void execute(DomainID requestId, LocalDate currentDate) {
        DonationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));

        if (currentDate.isAfter(request.getDateLimit())) {
            throw new IllegalStateException("Cannot notify donors. The donation request has already expired.");
        }

        if (request.isGoalReached()) {
            throw new IllegalStateException("Cannot notify donors. The goal for this request has already been reached.");
        }

        List<Donor> allDonors = donorRepository.findAll();
        List<Donor> eligibleDonors = matchingService.findEligibleDonors(request, allDonors, currentDate);

        if (eligibleDonors.isEmpty()) {
            System.out.println("Nenhum doador elegível encontrado para a requisição: " + requestId.getValue());
            return;
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
    }
}