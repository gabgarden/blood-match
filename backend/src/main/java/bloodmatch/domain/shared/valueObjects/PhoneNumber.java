package bloodmatch.domain.shared.valueObjects;

import java.util.Objects;

public class PhoneNumber {

    private final String value;

    public PhoneNumber(String value) {

        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Phone number cannot be null");

        if (!value.matches("\\d{10,13}"))
            throw new IllegalArgumentException("Invalid phone number");

        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PhoneNumber))
            return false;
        PhoneNumber that = (PhoneNumber) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}