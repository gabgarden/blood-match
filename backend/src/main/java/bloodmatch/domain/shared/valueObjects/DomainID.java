package bloodmatch.domain.shared.valueObjects;

import java.util.Objects;
import java.util.UUID;

public class DomainID {

    private final UUID value;

    public DomainID() {
        this.value = UUID.randomUUID();
    }

    public DomainID(UUID value) {

        if (value == null) {
            throw new IllegalArgumentException("DomainID cannot be null");
        }

        this.value = value;
    }

    public UUID getValue() {
        return value;
    }

    public static DomainID generate() {
        return new DomainID(UUID.randomUUID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DomainID domainID = (DomainID) o;
        return value.equals(domainID.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}