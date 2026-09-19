package bloodmatch.role.domain.organization.bloodcenter;

import bloodmatch.party.domain.Organization;
import bloodmatch.role.domain.organization.OrganizationRole;
import bloodmatch.shared.domain.valueObjects.DomainID;

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
