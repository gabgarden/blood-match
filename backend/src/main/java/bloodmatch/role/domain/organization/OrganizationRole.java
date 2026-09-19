package bloodmatch.role.domain.organization;

import bloodmatch.role.domain.PartyRole;
import bloodmatch.party.domain.Organization;
import bloodmatch.shared.domain.valueObjects.DomainID;

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