package bloodmatch.domain.roles.organization.bloodcenter;

import bloodmatch.domain.party.Organization;
import bloodmatch.domain.roles.organization.OrganizationRole;
import bloodmatch.domain.shared.valueObjects.DomainID;

public class BloodCenter extends OrganizationRole {

    public BloodCenter(Organization organization) {
        super(organization);
    }

    protected BloodCenter(Organization organization, DomainID id) {
        super(organization, id);
    }

    public static BloodCenter reconstitute(Organization organization, DomainID id) {
        return reconstitute(organization, id, null);
    }

    public static BloodCenter reconstitute(Organization organization, DomainID id, Long version) {
        BloodCenter bloodCenter = new BloodCenter(organization, id);
        bloodCenter.setVersion(version);
        return bloodCenter;
    }
}
