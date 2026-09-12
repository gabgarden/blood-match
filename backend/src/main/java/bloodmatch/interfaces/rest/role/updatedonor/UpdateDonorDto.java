package bloodmatch.interfaces.rest.role.updatedonor;

public record UpdateDonorDto(
    String personId,
    Long version,
    String bloodType,
    Double weight,
    Double maxDistanceInKm) {
}
