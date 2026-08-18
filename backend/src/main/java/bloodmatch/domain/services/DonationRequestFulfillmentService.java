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
 * Snapshot em memória do preenchimento das solicitações de um hemocentro.
 *
 * Não há vínculo doação → solicitação e nada é persistido. O serviço lê o
 * banco naquele instante e reparte as doações COMPLETED em FIFO: a solicitação
 * mais antiga compatível recebe a bolsa, até atingir a meta.
 */
@Service
public class DonationRequestFulfillmentService {

  private static final Comparator<DonationRequest> OLDEST_REQUEST_FIRST =
      Comparator.comparing(DonationRequest::getDateRequested)
          .thenComparing(request -> request.getId().getValue());

  private static final Comparator<Donation> OLDEST_DONATION_FIRST =
      Comparator.comparing(Donation::getDonationDate)
          .thenComparing(donation -> donation.getId().getValue());

  private final DonationRequestRepositoryInterface requestRepository;
  private final DonationRepositoryInterface donationRepository;

  public DonationRequestFulfillmentService(
      DonationRequestRepositoryInterface requestRepository,
      DonationRepositoryInterface donationRepository) {
    this.requestRepository = requestRepository;
    this.donationRepository = donationRepository;
  }

  /**
   * Preenchimento de um hemocentro no intervalo [startDate, endDate].
   *
   * 1. lê todas as solicitações daquele hemocentro
   * 2. lê as doações COMPLETED daquele hemocentro no intervalo
   * 3. ordena ambos do mais antigo para o mais novo
   * 4. cada doação vai para a primeira solicitação que ainda cabe
   *    e aceita o tipo sanguíneo
   */
  public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
      BloodCenter bloodCenter,
      LocalDate startDate,
      LocalDate endDate) {
    requireInterval(bloodCenter, startDate, endDate);

    DomainID organizationId = bloodCenter.getOrganization().getId();
    List<DonationRequest> requests = requestRepository.findByOrganizationId(organizationId);
    List<Donation> donations = donationRepository.findCompletedDonationsByOrganizationIdAndDateRange(
        organizationId,
        startOfPool(startDate, requests),
        endDate);

    return allocateFifo(requests, donations, endDate);
  }

  /**
   * Mesmo FIFO, quando a entrada mistura hemocentros.
   * Cada hemocentro tem o próprio pool: uma doação nunca preenche
   * solicitação de outro centro.
   */
  public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
      List<DonationRequest> requests,
      LocalDate startDate,
      LocalDate endDate) {
    requireInterval(requests, startDate, endDate);

    List<BloodCenter> bloodCenters = bloodCentersOf(requests);
    if (bloodCenters.isEmpty()) {
      return Map.of();
    }

    List<DomainID> organizationIds = organizationIdsOf(bloodCenters);
    List<DonationRequest> allRequests = requestRepository.findByOrganizationIds(organizationIds);
    List<Donation> allDonations =
        donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(
            organizationIds,
            startOfPool(startDate, allRequests),
            endDate);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> snapshot = new HashMap<>();
    for (BloodCenter bloodCenter : bloodCenters) {
      snapshot.putAll(allocateFifo(
          requestsAt(bloodCenter, allRequests),
          donationsAt(bloodCenter, allDonations),
          endDate));
    }
    return snapshot;
  }

  /**
   * FIFO: cada doação é atribuída à primeira solicitação que a aceita
   * e ainda não atingiu a meta. O {@code break} garante uma bolsa por doação.
   */
  private static Map<DomainID, DonationRequestFulfillmentStatusRecord> allocateFifo(
      List<DonationRequest> requests,
      List<Donation> donations,
      LocalDate asOfDate) {
    List<DonationRequest> requestsOldestFirst = sorted(requests, OLDEST_REQUEST_FIRST);
    List<Donation> donationsOldestFirst = sorted(donations, OLDEST_DONATION_FIRST);

    Map<DomainID, Integer> bags = new LinkedHashMap<>();
    for (DonationRequest request : requestsOldestFirst) {
      bags.put(request.getId(), 0);
    }

    for (Donation donation : donationsOldestFirst) {
      for (DonationRequest request : requestsOldestFirst) {
        int given = bags.get(request.getId());
        boolean hasRoom = given < request.getGoalBloodBags();
        if (hasRoom && request.acceptsDonation(donation, asOfDate)) {
          bags.put(request.getId(), given + 1);
          break;
        }
      }
    }

    Map<DomainID, DonationRequestFulfillmentStatusRecord> snapshot = new HashMap<>();
    for (DonationRequest request : requestsOldestFirst) {
      int given = bags.get(request.getId());
      snapshot.put(
          request.getId(),
          new DonationRequestFulfillmentStatusRecord(given, given >= request.getGoalBloodBags()));
    }
    return snapshot;
  }

  /**
   * Doações anteriores à solicitação mais antiga não preenchem ninguém.
   * Se essa data for antes de {@code startDate}, o pool começa nela.
   */
  private static LocalDate startOfPool(LocalDate startDate, List<DonationRequest> requests) {
    LocalDate start = startDate;
    for (DonationRequest request : requests) {
      if (request.getDateRequested().isBefore(start)) {
        start = request.getDateRequested();
      }
    }
    return start;
  }

  private static List<BloodCenter> bloodCentersOf(List<DonationRequest> requests) {
    List<BloodCenter> bloodCenters = new ArrayList<>();
    Set<DomainID> seen = new LinkedHashSet<>();
    for (DonationRequest request : requests) {
      BloodCenter bloodCenter = request.getBloodCenter();
      if (seen.add(bloodCenter.getOrganization().getId())) {
        bloodCenters.add(bloodCenter);
      }
    }
    return bloodCenters;
  }

  private static List<DomainID> organizationIdsOf(List<BloodCenter> bloodCenters) {
    List<DomainID> ids = new ArrayList<>();
    for (BloodCenter bloodCenter : bloodCenters) {
      ids.add(bloodCenter.getOrganization().getId());
    }
    return ids;
  }

  private static List<DonationRequest> requestsAt(
      BloodCenter bloodCenter,
      List<DonationRequest> requests) {
    DomainID organizationId = bloodCenter.getOrganization().getId();
    List<DonationRequest> result = new ArrayList<>();
    for (DonationRequest request : requests) {
      if (request.getBloodCenter().getOrganization().getId().equals(organizationId)) {
        result.add(request);
      }
    }
    return result;
  }

  private static List<Donation> donationsAt(BloodCenter bloodCenter, List<Donation> donations) {
    DomainID organizationId = bloodCenter.getOrganization().getId();
    List<Donation> result = new ArrayList<>();
    for (Donation donation : donations) {
      if (donation.getBloodCenter().getOrganization().getId().equals(organizationId)) {
        result.add(donation);
      }
    }
    return result;
  }

  private static <T> List<T> sorted(List<T> values, Comparator<T> order) {
    List<T> copy = new ArrayList<>(values);
    copy.sort(order);
    return copy;
  }

  private static void requireInterval(BloodCenter bloodCenter, LocalDate startDate, LocalDate endDate) {
    if (bloodCenter == null) {
      throw new IllegalArgumentException("Blood center cannot be null");
    }
    requireInterval(startDate, endDate);
  }

  private static void requireInterval(List<DonationRequest> requests, LocalDate startDate, LocalDate endDate) {
    if (requests == null) {
      throw new IllegalArgumentException("Requests cannot be null");
    }
    requireInterval(startDate, endDate);
  }

  private static void requireInterval(LocalDate startDate, LocalDate endDate) {
    if (startDate == null) {
      throw new IllegalArgumentException("Start date cannot be null");
    }
    if (endDate == null) {
      throw new IllegalArgumentException("End date cannot be null");
    }
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("End date cannot be before start date");
    }
  }
}
