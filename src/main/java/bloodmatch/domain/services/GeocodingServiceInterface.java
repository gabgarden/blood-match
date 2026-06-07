package main.java.bloodmatch.domain.services;

import bloodmatch.domain.shared.valueObjects.Address;

public interface GeocodingServiceInterface {
    /**
     * @param address Endereço base para busca
     * @return Novo endereço com coordenadas ou o mesmo endereço caso falhe
     */
    Address getCoordinatesFromAddress(Address address);
}