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

    private static final String GOOGLE_GEOCODING_API_URL =
            "https://maps.googleapis.com/maps/api/geocode/json";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GoogleMapsGeocodingService(
            @Value("${google.maps.api-key}") String apiKey) {

        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
    }

    @Override
    public Address getCoordinatesFromAddress(Address address) {

        if (address == null) {
            return null;
        }

        if (address.hasCoordinates()) {
            return address;
        }

        try {

            String fullAddress = address.getFullAddressAsString();

            Address geocoded =
                    tryGeocode(fullAddress, address.getZipCode());

            if (geocoded != null) {
                return copyCoordinates(address, geocoded);
            }

            System.out.println("Tentando busca alternativa...");

            String fallbackAddress =
                    address.getStreet()
                            + ", "
                            + address.getCity()
                            + ", "
                            + address.getState();

            geocoded =
                    tryGeocode(fallbackAddress, address.getZipCode());

            if (geocoded != null) {
                return copyCoordinates(address, geocoded);
            }

        } catch (Exception e) {

            System.err.println(
                    "Erro ao buscar coordenadas no Google Maps: "
                            + e.getMessage());

            e.printStackTrace();
        }

        return address;
    }

    private Address tryGeocode(
            String searchAddress,
            String expectedZipCode) {

        try {

            String url = UriComponentsBuilder
                    .fromHttpUrl(GOOGLE_GEOCODING_API_URL)
                    .queryParam("address", searchAddress)
                    .queryParam("key", apiKey)
                    .toUriString();

            System.out.println("======================================");
            System.out.println("GEOCODING");
            System.out.println("Buscando: " + searchAddress);
            System.out.println("URL:");
            System.out.println(url);

            JsonNode response =
                    restTemplate.getForObject(url, JsonNode.class);

            if (response == null) {

                System.out.println("Resposta nula do Google");
                return null;
            }

            System.out.println(response.toPrettyString());

            String status = response.path("status").asText();

            System.out.println("Status Google: " + status);

            if (!"OK".equals(status)) {

                if (response.has("error_message")) {

                    System.out.println(
                            "Erro Google: "
                                    + response.path("error_message").asText());
                }

                return null;
            }

            JsonNode results = response.path("results");

            if (results.isEmpty()) {

                System.out.println("Nenhum resultado encontrado.");
                return null;
            }

            System.out.println(
                    "Quantidade de resultados: "
                            + results.size());

            for (int i = 0; i < results.size(); i++) {

                JsonNode item = results.get(i);

                System.out.println(
                        "Resultado [" + i + "]: "
                                + item.path("formatted_address").asText());
            }

            JsonNode result = results.get(0);

            System.out.println("Primeiro resultado completo:");
            System.out.println(result.toPrettyString());

            String formattedAddress =
                    result.path("formatted_address").asText();

            System.out.println(
                    "Selecionado: "
                            + formattedAddress);

            if ("Brazil".equalsIgnoreCase(formattedAddress)
                    || "Brasil".equalsIgnoreCase(formattedAddress)) {

                System.out.println(
                        "Resultado genérico ignorado (Brasil).");

                return null;
            }

            boolean partialMatch =
                    result.path("partial_match")
                            .asBoolean(false);

            if (partialMatch) {

                System.out.println(
                        "Resultado ignorado: partial_match=true");

                return null;
            }

            if (expectedZipCode != null
                    && !expectedZipCode.isBlank()
                    && !zipCodeMatches(result, expectedZipCode)) {

                System.out.println(
                        "Resultado ignorado: CEP divergente.");

                return null;
            }

            JsonNode location =
                    result.path("geometry")
                            .path("location");

            double latitude =
                    location.path("lat").asDouble();

            double longitude =
                    location.path("lng").asDouble();

            System.out.println(
                    "Lat: "
                            + latitude
                            + " | Lng: "
                            + longitude);

            System.out.println("======================================");

            return new Address(
                    "",
                    "",
                    "",
                    "",
                    latitude,
                    longitude);

        } catch (Exception e) {

            System.err.println(
                    "Erro na tentativa de geocoding: "
                            + e.getMessage());

            e.printStackTrace();

            return null;
        }
    }

    private boolean zipCodeMatches(
            JsonNode result,
            String expectedZipCode) {

        JsonNode components =
                result.path("address_components");

        for (JsonNode component : components) {

            JsonNode types =
                    component.path("types");

            for (JsonNode type : types) {

                if ("postal_code".equals(type.asText())) {

                    String returnedZipCode =
                            component.path("long_name")
                                    .asText();

                    System.out.println(
                            "CEP retornado: "
                                    + returnedZipCode);

                    System.out.println(
                            "CEP esperado: "
                                    + expectedZipCode);

                    return expectedZipCode.equals(
                            returnedZipCode);
                }
            }
        }

        System.out.println(
                "Nenhum CEP retornado pelo Google.");

        return false;
    }

    private Address copyCoordinates(
            Address original,
            Address geocoded) {

        return new Address(
                original.getStreet(),
                original.getCity(),
                original.getState(),
                original.getZipCode(),
                geocoded.getLatitude(),
                geocoded.getLongitude());
    }
}