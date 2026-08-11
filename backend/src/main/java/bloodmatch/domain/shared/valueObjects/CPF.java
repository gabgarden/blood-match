package bloodmatch.domain.shared.valueObjects;

import java.util.Objects;

public class CPF {

    private final String value;

    public CPF(String value) {

        if (value == null || value.isBlank())
            throw new IllegalArgumentException("CPF cannot be null");

        String normalized = value.replaceAll("[^0-9]", "");

        if (normalized.length() != 11)
            throw new IllegalArgumentException("Invalid CPF");

        this.value = normalized;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CPF))
            return false;
        CPF cpf = (CPF) o;
        return value.equals(cpf.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}