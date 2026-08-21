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
 * Lê o banco naquele instante e reparte doações COMPLETED em FIFO: a solicitação
 * mais antiga compatível recebe a bolsa, até atingir a meta.
 */
@Service
public class DonationRequestFulfillmentService {

  /** Ordem FIFO das solicitações: data do pedido, depois id (desempate estável). */
  private static final Comparator<DonationRequest> OLDEST_REQUEST_FIRST =
      Comparator.comparing(DonationRequest::getDateRequested)
          .thenComparing(request -> request.getId().getValue());

  /** Ordem FIFO das doações: data da doação, depois id (desempate estável). */
  private static final Comparator<Donation> OLDEST_DONATION_FIRST =
      Comparator.comparing(Donation::getDonationDate)
          .thenComparing(donation -> donation.getId().getValue());

  private final DonationRequestRepositoryInterface requestRepository;
  private final DonationRepositoryInterface donationRepository;

  /** Injeta os repositórios usados para montar o snapshot. */
  public DonationRequestFulfillmentService(
      DonationRequestRepositoryInterface requestRepository,
      DonationRepositoryInterface donationRepository) {
    this.requestRepository = requestRepository;
    this.donationRepository = donationRepository;
  }

  /**
   * Entrada por hemocentro: busca pedidos ativos daquele centro e devolve
   * quantas bolsas cada um “recebeu” no snapshot até {@code asOfDate}.
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
   * Entrada por lista de pedidos: descobre os hemocentros envolvidos, recarrega
   * só os pedidos ativos desses centros e monta o snapshot FIFO até {@code asOfDate}.
   * Pedidos expirados da lista de entrada não entram no pool.
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
   * Carrega doações COMPLETED na janela [pedido mais antigo, asOfDate] e,
   * para cada hemocentro, chama o FIFO só com pedidos/doações daquele centro.
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
   * Núcleo FIFO de um hemocentro: ordena pedidos e doações do mais antigo ao mais novo;
   * cada doação vai para o primeiro pedido que a aceita e ainda tem meta; o break
   * garante uma bolsa por doação. Devolve contagem e se a meta foi atingida.
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
   * Início da janela de busca no banco: data do pedido mais antigo (ou asOfDate
   * se for mais cedo). Doações anteriores a isso não entram no snapshot.
   */
  private static LocalDate startOfPool(List<DonationRequest> requests, LocalDate asOfDate) {
    return requests.stream()
        .map(DonationRequest::getDateRequested)
        .min(Comparator.naturalOrder())
        .map(minDate -> minDate.isBefore(asOfDate) ? minDate : asOfDate)
        .orElse(asOfDate);
  }

  /** Hemocentros distintos presentes na lista de pedidos (sem duplicar). */
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

  /** Ids de organização usados nas queries do repositório. */
  private static List<DomainID> organizationIdsOf(List<BloodCenter> bloodCenters) {
    return bloodCenters.stream()
        .map(bloodCenter -> bloodCenter.getOrganization().getId())
        .toList();
  }

  /** Filtra só os pedidos daquele hemocentro antes do FIFO. */
  private static List<DonationRequest> requestsAt(
      BloodCenter bloodCenter,
      List<DonationRequest> requests) {
    DomainID organizationId = bloodCenter.getOrganization().getId();
    return requests.stream()
        .filter(request -> request.getBloodCenter().getOrganization().getId().equals(organizationId))
        .toList();
  }

  /** Filtra só as doações daquele hemocentro antes do FIFO. */
  private static List<Donation> donationsAt(BloodCenter bloodCenter, List<Donation> donations) {
    DomainID organizationId = bloodCenter.getOrganization().getId();
    return donations.stream()
        .filter(donation -> donation.getBloodCenter().getOrganization().getId().equals(organizationId))
        .toList();
  }

  /** Cópia ordenada: não altera a lista original. */
  private static <T> List<T> sorted(List<T> values, Comparator<T> order) {
    List<T> copy = new ArrayList<>(values);
    copy.sort(order);
    return copy;
  }
}
