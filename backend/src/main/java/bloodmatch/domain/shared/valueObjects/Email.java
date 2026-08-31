package bloodmatch.domain.shared.valueObjects;

import java.util.Objects;

public class Email {

    private final String value;

    public Email(String value) {

        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Email cannot be null");

        if (!value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))
            throw new IllegalArgumentException("Invalid email");

        this.value = value.toLowerCase();
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Email))
            return false;
        Email email = (Email) o;
        return value.equals(email.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}