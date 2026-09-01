package bloodmatch.domain.shared.entity;

import bloodmatch.domain.shared.valueObjects.DomainID;

public abstract class DomainObject {

    protected DomainID id;
    protected Long version;

    public DomainID getId() {
        return id;
    }

    protected void setId(DomainID id) {
        if (id == null)
            throw new IllegalArgumentException("Domain id cannot be null");
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    protected void setVersion(Long version) {
        this.version = version;
    }

}