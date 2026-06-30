package bloodmatch.infra.external.google;

import bloodmatch.domain.services.GeocodingServiceInterface;
import bloodmatch.domain.shared.valueObjects.Address;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GoogleMapsGeocodingService implements GeocodingServiceInterface {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private static final String GOOGLE_GEOCODING_API_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    public GoogleMapsGeocodingService(@Value("${google.maps.api-key}") String apiKey) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
    }

    @Override
    public Address getCoordinatesFromAddress(Address address) {
        if (address == null) return null;
        if (address.hasCoordinates()) return address;

        try {
            String fullAddress = address.getFullAddressAsString();
            
            String url = UriComponentsBuilder.fromHttpUrl(GOOGLE_GEOCODING_API_URL)
                    .queryParam("address", fullAddress)
                    .queryParam("key", apiKey)
                    .toUriString();

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response != null && "OK".equals(response.path("status").asText())) {
            
                JsonNode location = response.path("results").get(0).path("geometry").path("location");
                
                Double lat = location.path("lat").asDouble();
                Double lng = location.path("lng").asDouble();

                return new Address(
                        address.getStreet(),
                        address.getCity(),
                        address.getState(),
                        address.getZipCode(),
                        lat,
                        lng
                    );
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar coordenadas no Google Maps: " + e.getMessage());
        }
        return address;
    }
}