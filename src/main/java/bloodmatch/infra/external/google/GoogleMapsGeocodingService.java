package bloodmatch.infra.external.google;

import bloodmatch.domain.services.GeocodingServiceInterface;
import bloodmatch.domain.shared.valueObjects.Address;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class GoogleMapsGeocodingService implements GeocodingServiceInterface {

    private static final String GOOGLE_GEOCODING_API_URL =
            "https://maps.googleapis.com/maps/api/geocode/json";

    // Ordem de precisão do location_type do Google (do melhor para o pior)
    private static final List<String> PRECISION_RANK = List.of(
            "ROOFTOP", "RANGE_INTERPOLATED", "GEOMETRIC_CENTER", "APPROXIMATE");

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

            Address geocoded = tryGeocode(fullAddress, address.getZipCode());
            if (geocoded != null) {
                return copyCoordinates(address, geocoded);
            }

            System.out.println("Tentando busca alternativa (sem CEP, com bairro)...");

            String fallbackAddress = address.getFallbackAddressAsString();

            geocoded = tryGeocode(fallbackAddress, address.getZipCode());
            if (geocoded != null) {
                return copyCoordinates(address, geocoded);
            }

            System.out.println(
                    "Nenhum resultado com precisão suficiente. "
                            + "Endereço será salvo sem coordenadas para revisão manual.");

        } catch (Exception e) {
            System.err.println(
                    "Erro ao buscar coordenadas no Google Maps: " + e.getMessage());
            e.printStackTrace();
        }

        // Importante: NÃO retornar coordenada de centro de cidade "às cegas".
        // Melhor não ter coordenada do que ter uma errada para um sistema
        // de recomendação por proximidade.
        return address;
    }

    private Address tryGeocode(String searchAddress, String expectedZipCode) {

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(GOOGLE_GEOCODING_API_URL)
                    .queryParam("address", searchAddress)
                    .queryParam("components", "country:BR")
                    .queryParam("key", apiKey)
                    .toUriString();

            System.out.println("======================================");
            System.out.println("GEOCODING");
            System.out.println("Buscando: " + searchAddress);

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response == null) {
                System.out.println("Resposta nula do Google");
                return null;
            }

            String status = response.path("status").asText();
            System.out.println("Status Google: " + status);

            if (!"OK".equals(status)) {
                if (response.has("error_message")) {
                    System.out.println("Erro Google: " + response.path("error_message").asText());
                }
                return null;
            }

            JsonNode results = response.path("results");
            if (results.isEmpty()) {
                System.out.println("Nenhum resultado encontrado.");
                return null;
            }

            System.out.println("Quantidade de resultados: " + results.size());

            JsonNode zipMatchResult = null;
            JsonNode bestPrecisionResult = null;
            int bestPrecisionIndex = Integer.MAX_VALUE;

            boolean hasZip = expectedZipCode != null && !expectedZipCode.isBlank();

            for (JsonNode result : results) {

                String formattedAddress = result.path("formatted_address").asText();

                if (isGenericCountryResult(formattedAddress)) {
                    System.out.println("Ignorando resultado genérico: " + formattedAddress);
                    continue;
                }

                System.out.println("Candidato: " + formattedAddress
                        + " | location_type=" + result.path("geometry").path("location_type").asText()
                        + " | partial_match=" + result.path("partial_match").asBoolean(false));

                if (hasZip && zipCodeMatches(result, expectedZipCode)) {
                    zipMatchResult = result;
                    break;
                }

                String locationType = result.path("geometry").path("location_type").asText();
                int rank = PRECISION_RANK.indexOf(locationType);
                if (rank == -1) rank = PRECISION_RANK.size();

                if (!"APPROXIMATE".equals(locationType) && rank < bestPrecisionIndex) {
                    bestPrecisionResult = result;
                    bestPrecisionIndex = rank;
                }
            }

            JsonNode chosen = zipMatchResult != null ? zipMatchResult
                    : (!hasZip ? bestPrecisionResult : null);

            if (chosen == null) {
                System.out.println("Nenhum resultado passou nos critérios de aceitação.");
                return null;
            }

            JsonNode location = chosen.path("geometry").path("location");
            double latitude = location.path("lat").asDouble();
            double longitude = location.path("lng").asDouble();

            System.out.println("Selecionado: " + chosen.path("formatted_address").asText());
            System.out.println("Lat: " + latitude + " | Lng: " + longitude);
            System.out.println("======================================");

            return new Address("", "", "", "", "", "", latitude, longitude);

        } catch (Exception e) {
            System.err.println("Erro na tentativa de geocoding: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean isGenericCountryResult(String formattedAddress) {
        return "Brazil".equalsIgnoreCase(formattedAddress)
                || "Brasil".equalsIgnoreCase(formattedAddress);
    }

    private boolean zipCodeMatches(JsonNode result, String expectedZipCode) {

        String normalizedExpected = expectedZipCode.replaceAll("[^0-9]", "");

        JsonNode components = result.path("address_components");

        for (JsonNode component : components) {
            for (JsonNode type : component.path("types")) {
                if ("postal_code".equals(type.asText())) {
                    String returnedZipCode = component.path("long_name").asText()
                            .replaceAll("[^0-9]", "");
                    return normalizedExpected.equals(returnedZipCode);
                }
            }
        }
        return false;
    }

    private Address copyCoordinates(Address original, Address geocoded) {
        return new Address(
                original.getStreet(),
                original.getNumber(),
                original.getNeighborhood(),
                original.getCity(),
                original.getState(),
                original.getZipCode(),
                geocoded.getLatitude(),
                geocoded.getLongitude());
    }
}