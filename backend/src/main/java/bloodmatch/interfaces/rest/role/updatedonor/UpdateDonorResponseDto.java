package bloodmatch.interfaces.rest.role.updatedonor;

import bloodmatch.application.usecase.role.UpdateDonorUseCase;

import java.time.LocalDate;

public record UpdateDonorResponseDto(
    String id,
    String personId,
    Long version,
    String bloodType,
    Double weight,
    LocalDate weightUpdatedAt,
    Double maxDistanceInKm) {

  public static UpdateDonorResponseDto from(UpdateDonorUseCase.Output output) {
    return new UpdateDonorResponseDto(
        output.id(),
        output.personId(),
        output.version(),
        output.bloodType(),
        output.weight(),
        output.weightUpdatedAt(),
        output.maxDistanceInKm());
  }
}
