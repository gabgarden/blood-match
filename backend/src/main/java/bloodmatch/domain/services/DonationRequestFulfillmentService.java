package bloodmatch.domain.services;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.services.records.DonationRequestFulfillmentStatusRecord;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

/**
 * Snapshot in-memory of how completed donations fill requests at one blood
 * center. Nothing is persisted; callers pass a date window and receive
 * counters for that snapshot.
 */
@Service
public class DonationRequestFulfillmentService {

  private static final Comparator<DonationRequest> REQUEST_ORDER =
      Comparator.comparing(DonationRequest::getDateRequested)
          .thenComparing(request -> request.getId().getValue());

  private static final Comparator<Donation> DONATION_ORDER =
      Comparator.comparing(Donation::getDonationDate)
          .thenComparing(donation -> donation.getId().getValue());

  private final DonationRequestRepositoryInterface requestRepository;
  private final DonationRepositoryInterface donationRepository;

  public DonationRequestFulfillmentService(
      DonationRequestRepositoryInterface requestRepository,
      DonationRepositoryInterface donationRepository) {
    if (requestRepository == null)
      throw new IllegalArgumentException("DonationRequestRepository cannot be null");
    if (donationRepository == null)
      throw new IllegalArgumentException("DonationRepository cannot be null");
    this.requestRepository = requestRepository;
    this.donationRepository = donationRepository;
  }

  /**
   * Loads every request and completed donation of the blood center in
   * {@code [startDate, endDate]} and allocates FIFO in memory.
   */
  public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
      BloodCenter bloodCenter,
      LocalDate startDate,
      LocalDate endDate) {
    requireWindow(bloodCenter, startDate, endDate);

    DomainID organizationId = bloodCenter.getOrganization().getId();
    List<DonationRequest> requests = requestRepository.findByOrganizationId(organizationId);
    LocalDate from = earliest(startDate, requests);
    List<Donation> donations =
        donationRepository.findCompletedDonationsByOrganizationIdAndDateRange(
            organizationId, from, endDate);

    return fill(bloodCenter, from, endDate, requests, donations);
  }

  /**
   * Snapshot for every distinct blood center present in {@code requests}.
   * Each center is loaded in full so FIFO stays correct across all requests
   * at that hemocentro, not only the ones already in hand.
   */
  public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
      List<DonationRequest> requests,
      LocalDate startDate,
      LocalDate endDate) {
    if (requests == null)
      throw new IllegalArgumentException("Requests cannot be null");
    if (startDate == null)
      throw new IllegalArgumentException("Start date cannot be null");
    if (endDate == null)
      throw new IllegalArgumentException("End date cannot be null");

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result = new HashMap<>();
    Set<DomainID> seenOrganizations = new LinkedHashSet<>();
    for (DonationRequest request : requests) {
      BloodCenter bloodCenter = request.getBloodCenter();
      DomainID organizationId = bloodCenter.getOrganization().getId();
      if (!seenOrganizations.add(organizationId))
        continue;
      result.putAll(fill(bloodCenter, startDate, endDate));
    }
    return result;
  }

  /**
   * Same allocation as {@link #fill(BloodCenter, LocalDate, LocalDate)}, using
   * an already loaded snapshot. Used by tests and by callers that already
   * grouped data by blood center.
   */
  public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
      BloodCenter bloodCenter,
      LocalDate startDate,
      LocalDate endDate,
      List<DonationRequest> requests,
      List<Donation> donations) {
    requireWindow(bloodCenter, startDate, endDate);
    if (requests == null)
      throw new IllegalArgumentException("Requests cannot be null");
    if (donations == null)
      throw new IllegalArgumentException("Donations cannot be null");

    List<DonationRequest> centerRequests = new ArrayList<>();
    for (DonationRequest request : requests) {
      if (sameBloodCenter(request.getBloodCenter(), bloodCenter)) {
        centerRequests.add(request);
      }
    }
    centerRequests.sort(REQUEST_ORDER);

    List<Donation> centerDonations = new ArrayList<>();
    for (Donation donation : donations) {
      if (!sameBloodCenter(donation.getBloodCenter(), bloodCenter))
        continue;
      if (!inRange(donation.getDonationDate(), startDate, endDate))
        continue;
      centerDonations.add(donation);
    }
    centerDonations.sort(DONATION_ORDER);

    Map<DomainID, Integer> fulfilled = new LinkedHashMap<>();
    for (DonationRequest request : centerRequests) {
      fulfilled.put(request.getId(), 0);
    }

    for (Donation donation : centerDonations) {
      for (DonationRequest request : centerRequests) {
        if (!request.acceptsDonation(donation, endDate))
          continue;

        int current = fulfilled.get(request.getId());
        if (current >= request.getGoalBloodBags())
          continue;

        fulfilled.put(request.getId(), current + 1);
        break;
      }
    }

    Map<DomainID, DonationRequestFulfillmentStatusRecord> result = new HashMap<>();
    for (DonationRequest request : centerRequests) {
      int bags = fulfilled.get(request.getId());
      result.put(
          request.getId(),
          new DonationRequestFulfillmentStatusRecord(
              bags,
              bags >= request.getGoalBloodBags()));
    }
    return result;
  }

  private static LocalDate earliest(LocalDate startDate, List<DonationRequest> requests) {
    LocalDate from = startDate;
    for (DonationRequest request : requests) {
      if (request.getDateRequested().isBefore(from)) {
        from = request.getDateRequested();
      }
    }
    return from;
  }

  private static boolean sameBloodCenter(BloodCenter left, BloodCenter right) {
    return left.getOrganization().getId().equals(right.getOrganization().getId());
  }

  private static boolean inRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
    return !date.isBefore(startDate) && !date.isAfter(endDate);
  }

  private static void requireWindow(
      BloodCenter bloodCenter,
      LocalDate startDate,
      LocalDate endDate) {
    if (bloodCenter == null)
      throw new IllegalArgumentException("Blood center cannot be null");
    if (startDate == null)
      throw new IllegalArgumentException("Start date cannot be null");
    if (endDate == null)
      throw new IllegalArgumentException("End date cannot be null");
    if (endDate.isBefore(startDate))
      throw new IllegalArgumentException("End date cannot be before start date");
  }
}
