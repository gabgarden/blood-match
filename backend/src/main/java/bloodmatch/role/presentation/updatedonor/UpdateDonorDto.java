package bloodmatch.role.presentation.updatedonor;

public record UpdateDonorDto(
    String personId,
    Long version,
    String bloodType,
    Double weight,
    Double maxDistanceInKm) {
}
