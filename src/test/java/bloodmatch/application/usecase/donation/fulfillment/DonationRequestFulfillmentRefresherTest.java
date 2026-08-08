package bloodmatch.application.usecase.donation.fulfillment;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.repositories.DonationRepositoryInterface;
import bloodmatch.domain.repositories.DonationRequestRepositoryInterface;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DonationRequestFulfillmentRefresherTest {

  private final DonationRequestRepositoryInterface donationRequestRepository =
      mock(DonationRequestRepositoryInterface.class);
  private final DonationRepositoryInterface donationRepository = mock(DonationRepositoryInterface.class);
  private final DonationRequestFulfillmentService fulfillmentService =
      mock(DonationRequestFulfillmentService.class);
  private final DonationRequestFulfillmentRefresher refresher = new DonationRequestFulfillmentRefresher(
      donationRequestRepository,
      donationRepository,
      fulfillmentService);

  @Test
  void skipsDonationLookupAndSaveWhenThereAreNoActiveRequests() {
    DomainID organizationId = DomainID.generate();
    LocalDate currentDate = LocalDate.of(2026, 8, 7);

    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(organizationId), currentDate))
        .thenReturn(List.of());

    refresher.refresh(organizationId, currentDate);

    verify(donationRepository, never())
        .findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(anyList());
    verify(fulfillmentService, never()).synchronize(anyList(), anyList(), any());
    verify(donationRequestRepository, never()).save(any(DonationRequest.class));
  }

  @Test
  void synchronizesAndPersistsWhenActiveRequestsExist() {
    DomainID organizationId = DomainID.generate();
    LocalDate currentDate = LocalDate.of(2026, 8, 7);
    DonationRequest request = mock(DonationRequest.class);
    List<Donation> donations = List.of(mock(Donation.class));

    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(organizationId), currentDate))
        .thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
            List.of(organizationId)))
        .thenReturn(donations);

    refresher.refresh(organizationId, currentDate);

    verify(fulfillmentService).synchronize(List.of(request), donations, currentDate);
    verify(donationRequestRepository).save(request);
  }
}
