package bloodmatch.domain.roles.organization;

import bloodmatch.domain.roles.PartyRole;
import bloodmatch.domain.party.Organization;
import bloodmatch.domain.shared.valueObjects.DomainID;

public abstract class OrganizationRole extends PartyRole<Organization> {

  protected OrganizationRole(Organization organization) {
    super(organization);
  }

  protected OrganizationRole(Organization organization, DomainID id) {
    super(organization, id);
  }

  public Organization getOrganization() {
    return getParty();
  }

}