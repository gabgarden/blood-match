package bloodmatch.application.usecase.donationrequest.notification;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.matching.DonorMatchingService;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.repositories.DonorRepositoryInterface;
import bloodmatch.domain.repositories.UserAccountRepositoryInterface;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.services.NotificationServiceInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class NotifyPotentialDonorsUseCase {

    private final DonationRequestRepositoryInterface requestRepository;
    private final DonationRepositoryInterface donationRepository;
    private final DonorRepositoryInterface donorRepository;
    private final UserAccountRepositoryInterface userAccountRepository;
    private final DonationRequestFulfillmentService fulfillmentService;
    private final DonorMatchingService matchingService;
    private final NotificationServiceInterface notificationService;

    public NotifyPotentialDonorsUseCase(
            DonationRequestRepositoryInterface requestRepository,
            DonationRepositoryInterface donationRepository,
            DonorRepositoryInterface donorRepository,
            UserAccountRepositoryInterface userAccountRepository,
            DonationRequestFulfillmentService fulfillmentService,
            NotificationServiceInterface notificationService) {
        
        this.requestRepository = requestRepository;
        this.donationRepository = donationRepository;
        this.donorRepository = donorRepository;
        this.userAccountRepository = userAccountRepository;
        this.fulfillmentService = fulfillmentService;
        this.notificationService = notificationService;
        
        // Instanciando o serviço de domínio que aplica os filtros
        this.matchingService = new DonorMatchingService(); 
    }

    public void execute(DomainID requestId, LocalDate currentDate) {
        DonationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));

        // 1. Validação: A requisição já expirou?
        if (currentDate.isAfter(request.getDateLimit())) {
            throw new IllegalStateException("Cannot notify donors. The donation request has already expired.");
        }

        // 2. Validação: A meta já foi atingida?
        List<Donation> donations = donationRepository
                .findCompletedDonationsForBloodCentersOrderedByDonationDateAsc(
                        List.of(request.getBloodCenter().getOrganization().getId())
                );

        var fulfillmentMap = fulfillmentService.calculate(List.of(request), donations, currentDate);
        var fulfillmentStatus = fulfillmentMap.get(request.getId());

        if (fulfillmentStatus != null && fulfillmentStatus.goalReached()) {
            throw new IllegalStateException("Cannot notify donors. The goal for this request has already been reached.");
        }

        // 3. Buscar e filtrar doadores elegíveis
        List<Donor> allDonors = donorRepository.findAll();
        List<Donor> eligibleDonors = matchingService.findEligibleDonors(request, allDonors, currentDate);

        if (eligibleDonors.isEmpty()) {
            System.out.println("Nenhum doador elegível encontrado para a requisição: " + requestId.getValue());
            return;
        }

        // 4. Disparar notificações buscando a conta de cada doador
        for (Donor donor : eligibleDonors) {
            // Obtém o partyId associado ao doador
            DomainID partyId = donor.getPerson().getId();
            
            // Busca a conta de usuário pelo partyId
            UserAccount account = userAccountRepository.findByPartyId(partyId)
                    .orElse(null);

            // Verifica se a conta existe e está ativa antes de notificar
            if (account != null && account.isEnabled()) {
                notificationService.notifyDonorAboutRequest(donor, account, request);
            } else {
                System.out.println("Conta inativa ou não encontrada para o doador: " + donor.getPerson().getName());
            }
        }
    }
}