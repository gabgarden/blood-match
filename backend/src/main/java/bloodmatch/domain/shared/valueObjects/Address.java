package bloodmatch.domain.shared.valueObjects;

import java.util.Objects;

public class Address {

    private final String street;
    private final String city;
    private final String state;
    private final String zipCode;
    private final Double latitude;
    private final Double longitude;

    public Address(String street, String city, String state, String zipCode, Double latitude, Double longitude) {
        if (street == null || city == null || state == null) {
            throw new IllegalArgumentException("Address fields cannot be null");
        }
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // utilizado quando ainda NÃO tem as coordenadas (ex: cadastro inicial)
    public Address(String street, String city, String state, String zipCode) {
        this(street, city, state, zipCode, null, null);
    }

    // verificar se este endereço já foi geolocalizado
    public boolean hasCoordinates() {
        return this.latitude != null && this.longitude != null;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public Double getLatitude() {
        return latitude;
    }
    public Double getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Address))
            return false;
        Address address = (Address) o;
        return street.equals(address.street) &&
                city.equals(address.city) &&
                state.equals(address.state) &&
                Objects.equals(zipCode, address.zipCode) &&
                Objects.equals(latitude, address.latitude) &&
                Objects.equals(longitude, address.longitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, zipCode, latitude, longitude);
    }

    // "Rua X, 241, Bairro, Cidade, UF, CEP, Brasil"
    public String getFullAddressAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(street);

        sb.append(", ").append(city)
          .append(", ").append(state);

        if (zipCode != null && !zipCode.isBlank()) {
            sb.append(", ").append(zipCode);
        }

        sb.append(", Brasil");

        return sb.toString();
    }

    // "Rua X, 241, Bairro, Cidade, UF, Brasil" (sem CEP, usado no fallback)
    public String getFallbackAddressAsString() {
        StringBuilder sb = new StringBuilder();
        sb.append(street);

        sb.append(", ").append(city)
          .append(", ").append(state)
          .append(", Brasil");

        return sb.toString();
    }

    public final Double distanceTo(Address other) {
        if (!this.hasCoordinates() || other == null || !other.hasCoordinates()) {
            return null;
        }

        final int R = 6371; // Raio da terra em KM

        double latDistance = Math.toRadians(other.getLatitude() - this.getLatitude());
        double lonDistance = Math.toRadians(other.getLongitude() - this.getLongitude());

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.getLatitude()))
                * Math.cos(Math.toRadians(other.getLatitude()))
                * Math.sin(lonDistance / 2)
                * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}