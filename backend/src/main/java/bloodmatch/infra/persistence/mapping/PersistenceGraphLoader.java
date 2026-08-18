package bloodmatch.infra.persistence.mapping;

import bloodmatch.domain.donation.Donation;
import bloodmatch.domain.donationrequest.DonationRequest;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.party.Party;
import bloodmatch.domain.party.Person;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenter;
import bloodmatch.domain.roles.person.donor.Donor;
import bloodmatch.domain.roles.requester.Requester;
import bloodmatch.infra.persistence.repository.mongo.BloodCenterMongoRepository;
import bloodmatch.infra.persistence.repository.mongo.DonorMongoRepository;
import bloodmatch.infra.persistence.repository.mongo.PartyMongoRepository;
import bloodmatch.infra.persistence.repository.mongo.RequesterMongoRepository;
import bloodmatch.infra.persistence.schema.BloodCenterSchema;
import bloodmatch.infra.persistence.schema.DonationRequestSchema;
import bloodmatch.infra.persistence.schema.DonationSchema;
import bloodmatch.infra.persistence.schema.DonorSchema;
import bloodmatch.infra.persistence.schema.PartySchema;
import bloodmatch.infra.persistence.schema.RequesterSchema;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns Mongo documents into domain objects with a handful of bulk reads
 * instead of one findById per related party/role.
 */
@Component
public class PersistenceGraphLoader {

  private final PartyMongoRepository partyMongoRepository;
  private final RequesterMongoRepository requesterMongoRepository;
  private final BloodCenterMongoRepository bloodCenterMongoRepository;
  private final DonorMongoRepository donorMongoRepository;

  public PersistenceGraphLoader(
      PartyMongoRepository partyMongoRepository,
      RequesterMongoRepository requesterMongoRepository,
      BloodCenterMongoRepository bloodCenterMongoRepository,
      DonorMongoRepository donorMongoRepository) {
    this.partyMongoRepository = partyMongoRepository;
    this.requesterMongoRepository = requesterMongoRepository;
    this.bloodCenterMongoRepository = bloodCenterMongoRepository;
    this.donorMongoRepository = donorMongoRepository;
  }

  public List<DonationRequest> toDonationRequests(List<DonationRequestSchema> schemas) {
    if (schemas == null || schemas.isEmpty()) {
      return List.of();
    }

    Set<String> requesterPartyIds = new LinkedHashSet<>();
    Set<String> organizationIds = new LinkedHashSet<>();
    for (DonationRequestSchema schema : schemas) {
      requesterPartyIds.add(schema.getRequesterId());
      organizationIds.add(schema.getOrganizationId());
    }

    Set<String> partyIds = new LinkedHashSet<>();
    partyIds.addAll(requesterPartyIds);
    partyIds.addAll(organizationIds);
    Map<String, Party> parties = partiesById(partyIds);

    Map<String, Requester> requesters = requestersByPartyId(requesterPartyIds, parties);
    Map<String, BloodCenter> bloodCenters = bloodCentersByOrganizationId(organizationIds, parties);

    List<DonationRequest> requests = new ArrayList<>(schemas.size());
    for (DonationRequestSchema schema : schemas) {
      Requester requester = requesters.get(schema.getRequesterId());
      if (requester == null) {
        throw new IllegalArgumentException("Requester role not found");
      }
      BloodCenter bloodCenter = bloodCenters.get(schema.getOrganizationId());
      if (bloodCenter == null) {
        throw new IllegalArgumentException("Blood center role not found");
      }
      requests.add(schema.toDomain(requester, bloodCenter));
    }
    return requests;
  }

  public List<Donation> toDonations(List<DonationSchema> schemas) {
    if (schemas == null || schemas.isEmpty()) {
      return List.of();
    }

    Set<String> donorPersonIds = new LinkedHashSet<>();
    Set<String> organizationIds = new LinkedHashSet<>();
    for (DonationSchema schema : schemas) {
      donorPersonIds.add(schema.getDonorPersonId());
      organizationIds.add(schema.getOrganizationId());
    }

    Set<String> partyIds = new LinkedHashSet<>();
    partyIds.addAll(donorPersonIds);
    partyIds.addAll(organizationIds);
    Map<String, Party> parties = partiesById(partyIds);

    Map<String, Donor> donors = donorsByPersonId(donorPersonIds, parties);
    Map<String, BloodCenter> bloodCenters = bloodCentersByOrganizationId(organizationIds, parties);

    List<Donation> donations = new ArrayList<>(schemas.size());
    for (DonationSchema schema : schemas) {
      Donor donor = donors.get(schema.getDonorPersonId());
      if (donor == null) {
        throw new IllegalArgumentException("Donor role not found");
      }
      BloodCenter bloodCenter = bloodCenters.get(schema.getOrganizationId());
      if (bloodCenter == null) {
        throw new IllegalArgumentException("Blood center role not found");
      }
      donations.add(schema.toDomain(donor, bloodCenter));
    }
    return donations;
  }

  private Map<String, Party> partiesById(Collection<String> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    Map<String, Party> parties = new HashMap<>();
    for (PartySchema schema : partyMongoRepository.findAllById(ids)) {
      parties.put(schema.getId(), schema.toDomain());
    }
    return parties;
  }

  private Map<String, Requester> requestersByPartyId(
      Collection<String> partyIds,
      Map<String, Party> parties) {
    if (partyIds.isEmpty()) {
      return Map.of();
    }
    Map<String, Requester> requesters = new HashMap<>();
    for (RequesterSchema schema : requesterMongoRepository.findByPartyIdIn(partyIds)) {
      requesters.put(schema.getPartyId(), schema.toDomain(parties.get(schema.getPartyId())));
    }
    return requesters;
  }

  private Map<String, BloodCenter> bloodCentersByOrganizationId(
      Collection<String> organizationIds,
      Map<String, Party> parties) {
    if (organizationIds.isEmpty()) {
      return Map.of();
    }
    Map<String, BloodCenter> bloodCenters = new HashMap<>();
    for (BloodCenterSchema schema : bloodCenterMongoRepository.findByOrganizationIdIn(organizationIds)) {
      Party party = parties.get(schema.getOrganizationId());
      if (!(party instanceof Organization organization)) {
        throw new IllegalArgumentException("Organization not found");
      }
      bloodCenters.put(schema.getOrganizationId(), schema.toDomain(organization));
    }
    return bloodCenters;
  }

  private Map<String, Donor> donorsByPersonId(
      Collection<String> personIds,
      Map<String, Party> parties) {
    if (personIds.isEmpty()) {
      return Map.of();
    }
    Map<String, Donor> donors = new HashMap<>();
    for (DonorSchema schema : donorMongoRepository.findByPersonIdIn(personIds)) {
      Party party = parties.get(schema.getPersonId());
      if (!(party instanceof Person person)) {
        throw new IllegalArgumentException("Person not found");
      }
      donors.put(schema.getPersonId(), schema.toDomain(person));
    }
    return donors;
  }
}
