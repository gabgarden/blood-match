package bloodmatch.application.usecase.donation.fulfillment;

import bloodmatch.application.exception.ConflictException;
import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.services.DonationRequestFulfillmentService;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

  @Test
  void reloadsAndPersistsAfterOptimisticLockOnSave() {
    DomainID organizationId = DomainID.generate();
    LocalDate currentDate = LocalDate.of(2026, 8, 7);
    DonationRequest request = mock(DonationRequest.class);
    AtomicInteger sleeps = new AtomicInteger();
    DonationRequestFulfillmentRefresher retrying = new DonationRequestFulfillmentRefresher(
        donationRequestRepository,
        donationRepository,
        fulfillmentService,
        3,
        attempt -> sleeps.incrementAndGet());

    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(organizationId), currentDate))
        .thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
            List.of(organizationId)))
        .thenReturn(List.of());
    doThrow(lockConflict())
        .doNothing()
        .when(donationRequestRepository)
        .save(request);

    retrying.refresh(organizationId, currentDate);

    verify(donationRequestRepository, times(2))
        .findActiveRequestsByOrganizationIds(List.of(organizationId), currentDate);
    verify(donationRequestRepository, times(2)).save(request);
    verify(fulfillmentService, times(2)).synchronize(anyList(), anyList(), any());
    assertEquals(1, sleeps.get());
  }

  @Test
  void retriesTheWholeBatchWhenALaterSaveConflicts() {
    DomainID organizationId = DomainID.generate();
    LocalDate currentDate = LocalDate.of(2026, 8, 7);
    DonationRequest first = mock(DonationRequest.class);
    DonationRequest second = mock(DonationRequest.class);
    DonationRequestFulfillmentRefresher retrying = new DonationRequestFulfillmentRefresher(
        donationRequestRepository,
        donationRepository,
        fulfillmentService,
        3,
        attempt -> {
        });

    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(organizationId), currentDate))
        .thenReturn(List.of(first, second));
    when(donationRepository.findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
            List.of(organizationId)))
        .thenReturn(List.of());
    doNothing().when(donationRequestRepository).save(first);
    doThrow(lockConflict())
        .doNothing()
        .when(donationRequestRepository)
        .save(second);

    retrying.refresh(organizationId, currentDate);

    verify(donationRequestRepository, times(2))
        .findActiveRequestsByOrganizationIds(List.of(organizationId), currentDate);
    verify(donationRequestRepository, times(2)).save(first);
    verify(donationRequestRepository, times(2)).save(second);
  }

  @Test
  void throwsConflictWhenRetriesAreExhausted() {
    DomainID organizationId = DomainID.generate();
    LocalDate currentDate = LocalDate.of(2026, 8, 7);
    DonationRequest request = mock(DonationRequest.class);
    DonationRequestFulfillmentRefresher retrying = new DonationRequestFulfillmentRefresher(
        donationRequestRepository,
        donationRepository,
        fulfillmentService,
        2,
        attempt -> {
        });

    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(organizationId), currentDate))
        .thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
            List.of(organizationId)))
        .thenReturn(List.of());
    doThrow(lockConflict()).when(donationRequestRepository).save(request);

    ConflictException thrown = assertThrows(
        ConflictException.class,
        () -> retrying.refresh(organizationId, currentDate));

    assertEquals(DonationRequestFulfillmentRefresher.CONFLICT_MESSAGE, thrown.getMessage());
    verify(donationRequestRepository, times(2)).save(request);
  }

  @Test
  void doesNotRetryUnrelatedSaveFailures() {
    DomainID organizationId = DomainID.generate();
    LocalDate currentDate = LocalDate.of(2026, 8, 7);
    DonationRequest request = mock(DonationRequest.class);
    DonationRequestFulfillmentRefresher retrying = new DonationRequestFulfillmentRefresher(
        donationRequestRepository,
        donationRepository,
        fulfillmentService,
        3,
        attempt -> {
        });

    when(donationRequestRepository.findActiveRequestsByOrganizationIds(
            List.of(organizationId), currentDate))
        .thenReturn(List.of(request));
    when(donationRepository.findCompletedDonationsForOrganizationsOrderedByDonationDateAsc(
            List.of(organizationId)))
        .thenReturn(List.of());
    doThrow(new IllegalStateException("disk full")).when(donationRequestRepository).save(request);

    IllegalStateException thrown = assertThrows(
        IllegalStateException.class,
        () -> retrying.refresh(organizationId, currentDate));

    assertEquals("disk full", thrown.getMessage());
    verify(donationRequestRepository, times(1)).save(request);
  }

  private static IllegalStateException lockConflict() {
    return new IllegalStateException(
        "Donation request was changed by another operation. Reload it and try again.");
  }
}
