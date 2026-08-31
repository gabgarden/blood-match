package bloodmatch.domain.shared.valueObjects;

import java.util.Objects;

public class CNPJ {

    private final String value;

    public CNPJ(String value) {

        if (value == null || value.isBlank())
            throw new IllegalArgumentException("CNPJ cannot be null");

        String normalized = value.replaceAll("[^0-9]", "");

        if (normalized.length() != 14)
            throw new IllegalArgumentException("Invalid CNPJ");

        this.value = normalized;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CNPJ))
            return false;
        CNPJ cnpj = (CNPJ) o;
        return value.equals(cnpj.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}