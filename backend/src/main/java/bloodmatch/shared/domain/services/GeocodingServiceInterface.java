package bloodmatch.shared.domain.services;

import bloodmatch.shared.domain.valueObjects.Address;

public interface GeocodingServiceInterface {
    /**
     * @param address Endereço base para busca
     * @return Novo endereço com coordenadas ou o mesmo endereço caso falhe
     */
    Address getCoordinatesFromAddress(Address address);
}