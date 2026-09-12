package bloodmatch.domain.services;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donation.DonationRepositoryInterface;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.donationrequest.DonationRequestRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.services.records.DonationRequestFulfillmentStatusRecord;
import bloodmatch.domain.shared.valueObjects.DomainID;

import java.math.BigDecimal;
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
 * mais antiga compatível recebe a bolsa. A meta, porém, é liberada aos poucos —
 * cada solicitação só recebe até o teto proporcional ao tempo já decorrido da sua
 * janela; o que sobra é redistribuído numa segunda passagem FIFO até a meta cheia.
 */
@Service
public class DonationRequestFulfillmentService {


  //APENAS CRITÉRIO
  /** Ordem FIFO das solicitações: data do pedido, depois id (desempate estável). */
  private static final Comparator<DonationRequest> OLDEST_REQUEST_FIRST =
      Comparator.comparing(DonationRequest::getDateRequested)
          .thenComparing(request -> request.getId().getValue());


  //APENAS CRITÉRIO
  /** Ordem FIFO das doações: data da doação, depois id (desempate estável). */
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
   * para cada hemocentro, chama a alocação só com pedidos/doações daquele centro.
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
      snapshot.putAll(allocateProportionalThenFifo(
          requestsAt(bloodCenter, requests),
          donationsAt(bloodCenter, allDonations),
          asOfDate));
    }
    return snapshot;
  }

  /**
   * Núcleo de alocação de um hemocentro, em duas passagens sobre o mesmo pool.
   *
   * 1ª passagem — cada pedido só pode receber até o seu teto proporcional
   * ({@code proportionalGoalAt}): quem tem prazo folgado é represado e a bolsa
   * segue para o próximo pedido compatível.
   *
   * 2ª passagem — as doações que ninguém pôde absorver voltam ao início, na mesma
   * ordem FIFO, agora limitadas só pela meta cheia. É o que impede bolsas ociosas:
   * o total alocado continua igual ao do FIFO puro, o teto só muda quem recebe
   * quando há escassez.
   */
  private static Map<DomainID, DonationRequestFulfillmentStatusRecord> allocateProportionalThenFifo(
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

    List<Donation> leftovers = distribute(
        requestsOldestFirst,
        donationsOldestFirst,
        bags,
        proportionalLimits(requestsOldestFirst, asOfDate),
        asOfDate);

    distribute(
        requestsOldestFirst,
        leftovers,
        bags,
        fullGoalLimits(requestsOldestFirst),
        asOfDate);

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
   * Limite da 1ª passagem: quanto da meta o tempo já liberou para cada pedido.
   */
  private static Map<DomainID, BigDecimal> proportionalLimits(
      List<DonationRequest> requests,
      LocalDate asOfDate) {
    Map<DomainID, BigDecimal> limits = new LinkedHashMap<>();
    for (DonationRequest request : requests) {
      limits.put(request.getId(), request.proportionalGoalAt(asOfDate));
    }
    return limits;
  }

  /**
   * Limite da 2ª passagem: a meta cheia, sem represar ninguém.
   */
  private static Map<DomainID, BigDecimal> fullGoalLimits(List<DonationRequest> requests) {
    Map<DomainID, BigDecimal> limits = new LinkedHashMap<>();
    for (DonationRequest request : requests) {
      limits.put(request.getId(), BigDecimal.valueOf(request.getGoalBloodBags()));
    }
    return limits;
  }

  /**
   * Uma passagem FIFO: cada doação vai para o primeiro pedido que a aceita e ainda
   * não chegou ao limite daquela passagem; o break garante uma bolsa por doação.
   *
   * {@code limits} traz o teto de cada pedido já calculado — é só o que muda entre
   * as duas passagens. {@code bags} é acumulado entre elas. Devolve as doações que
   * não acharam destino, que são justamente a entrada da passagem seguinte.
   */
  private static List<Donation> distribute(
      List<DonationRequest> requestsOldestFirst,
      List<Donation> donationsOldestFirst,
      Map<DomainID, Integer> bags,
      Map<DomainID, BigDecimal> limits,
      LocalDate asOfDate) {
    List<Donation> unallocated = new ArrayList<>();
    for (Donation donation : donationsOldestFirst) {
      boolean allocated = false;
      for (DonationRequest request : requestsOldestFirst) {
        int given = bags.get(request.getId());
        BigDecimal limit = limits.get(request.getId());
        if (BigDecimal.valueOf(given).compareTo(limit) < 0 && request.acceptsDonation(donation, asOfDate)) {
          bags.put(request.getId(), given + 1);
          allocated = true;
          break;
        }
      }
      if (!allocated) {
        unallocated.add(donation);
      }
    }
    return unallocated;
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
