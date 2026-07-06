package bloodmatch.application.usecase.donation.acceptandcreatependingfromrequest;

import bloodmatch.application.usecase.donation.creatependingfromrequest.CreatePendingDonationFromRequestUseCase;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AcceptDonorAndCreatePendingDonationFromRequestUseCase {

  private final CreatePendingDonationFromRequestUseCase createPendingDonationFromRequestUseCase;

  public AcceptDonorAndCreatePendingDonationFromRequestUseCase(
      CreatePendingDonationFromRequestUseCase createPendingDonationFromRequestUseCase) {
    this.createPendingDonationFromRequestUseCase = createPendingDonationFromRequestUseCase;
  }

  @Transactional
  public Donation execute(
      DomainID requestId,
      DomainID personId,
      LocalDate expectedDate) {

    return execute(requestId, personId, expectedDate, LocalDate.now());
  }

  @Transactional
  public Donation execute(
      DomainID requestId,
      DomainID personId,
      LocalDate expectedDate,
      LocalDate currentDate) {

    if (requestId == null)
      throw new IllegalArgumentException("Request id cannot be null");
    if (personId == null)
      throw new IllegalArgumentException("Person id cannot be null");
    if (expectedDate == null)
      throw new IllegalArgumentException("Expected date cannot be null");
    if (currentDate == null)
      throw new IllegalArgumentException("Current date cannot be null");


    return createPendingDonationFromRequestUseCase.execute(
        personId,
        requestId,
        expectedDate,
        currentDate);
  }
}
