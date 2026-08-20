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
   * Snapshot do preenchimento das solicitações ativas de um hemocentro até {@code asOfDate}.
   * A janela de doações começa na solicitação ativa mais antiga, não no histórico inteiro.
   */
  public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
      BloodCenter bloodCenter,
      LocalDate asOfDate) {
    if (bloodCenter == null) {
      throw new IllegalArgumentException("Blood center cannot be null");
    }
    if (asOfDate == null) {
      throw new IllegalArgumentException("As of date cannot be null");
    }

    DomainID organizationId = bloodCenter.getOrganization().getId();
    List<DonationRequest> requests = requestRepository.findActiveRequestsByOrganizationIds(
        List.of(organizationId),
        asOfDate);
    if (requests.isEmpty()) {
      return Map.of();
    }

    return allocateLoaded(requests, asOfDate);
  }

  /**
   * Snapshot FIFO até {@code asOfDate} entre as solicitações ativas dos hemocentros
   * presentes em {@code requests}. Pedidos expirados não entram no pool.
   */
  public Map<DomainID, DonationRequestFulfillmentStatusRecord> fill(
      List<DonationRequest> requests,
      LocalDate asOfDate) {
    if (requests == null) {
      throw new IllegalArgumentException("Requests cannot be null");
    }
    if (asOfDate == null) {
      throw new IllegalArgumentException("As of date cannot be null");
    }

    List<BloodCenter> bloodCenters = bloodCentersOf(requests);
    if (bloodCenters.isEmpty()) {
      return Map.of();
    }

    List<DonationRequest> activeRequests = requestRepository.findActiveRequestsByOrganizationIds(
        organizationIdsOf(bloodCenters),
        asOfDate);
    if (activeRequests.isEmpty()) {
      return Map.of();
    }

    return allocateLoaded(activeRequests, asOfDate);
  }

  /**
   * Carrega as doações COMPLETED dos hemocentros de {@code requests} na janela
   * [pedido mais antigo, asOfDate] e reparte FIFO por centro.
   */
  private Map<DomainID, DonationRequestFulfillmentStatusRecord> allocateLoaded(
      List<DonationRequest> requests,
      LocalDate asOfDate) {
    List<BloodCenter> bloodCenters = bloodCentersOf(requests);
    if (bloodCenters.isEmpty()) {
      return Map.of();
    }

    List<Donation> allDonations =
        donationRepository.findCompletedDonationsByOrganizationIdsAndDateRange(
            organizationIdsOf(bloodCenters),
            startOfPool(requests, asOfDate),
            asOfDate);

    Map<DomainID, DonationRequestFulfillmentStatusRecord> snapshot = new HashMap<>();
    for (BloodCenter bloodCenter : bloodCenters) {
      snapshot.putAll(allocateFifo(
          requestsAt(bloodCenter, requests),
          donationsAt(bloodCenter, allDonations),
          asOfDate));
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
    List<Donation> donationsOldestFirst = sorted(
        donations.stream().filter(Donation::isCompleted).toList(),
        OLDEST_DONATION_FIRST);

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
   * Calcula a data inicial do pool a partir da solicitação mais antiga.
   * Doações anteriores a essa data não preenchem ninguém no snapshot.
   */
  private static LocalDate startOfPool(List<DonationRequest> requests, LocalDate asOfDate) {
    return requests.stream()
        .map(DonationRequest::getDateRequested)
        .min(Comparator.naturalOrder())
        .map(minDate -> minDate.isBefore(asOfDate) ? minDate : asOfDate)
        .orElse(asOfDate);
  }

  private static List<BloodCenter> bloodCentersOf(List<DonationRequest> requests) {
    Set<DomainID> seen = new LinkedHashSet<>();
    List<BloodCenter> bloodCenters = new ArrayList<>();
    for (DonationRequest request : requests) {
      BloodCenter bloodCenter = request.getBloodCenter();
      if (seen.add(bloodCenter.getOrganization().getId())) {
        bloodCenters.add(bloodCenter);
      }
    }
    return bloodCenters;
  }

  private static List<DomainID> organizationIdsOf(List<BloodCenter> bloodCenters) {
    return bloodCenters.stream()
        .map(bloodCenter -> bloodCenter.getOrganization().getId())
        .toList();
  }

  private static List<DonationRequest> requestsAt(
      BloodCenter bloodCenter,
      List<DonationRequest> requests) {
    DomainID organizationId = bloodCenter.getOrganization().getId();
    return requests.stream()
        .filter(request -> request.getBloodCenter().getOrganization().getId().equals(organizationId))
        .toList();
  }

  private static List<Donation> donationsAt(BloodCenter bloodCenter, List<Donation> donations) {
    DomainID organizationId = bloodCenter.getOrganization().getId();
    return donations.stream()
        .filter(donation -> donation.getBloodCenter().getOrganization().getId().equals(organizationId))
        .toList();
  }

  private static <T> List<T> sorted(List<T> values, Comparator<T> order) {
    List<T> copy = new ArrayList<>(values);
    copy.sort(order);
    return copy;
  }
}
