package bloodmatch.application.usecase.bloodcenter.schedule;

import bloodmatch.application.exception.NotFoundException;
import bloodmatch.application.exception.ValidationException;
import bloodmatch.application.shared.DomainIdParser;
import bloodmatch.application.shared.PartyOwnership;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterSchedule;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterScheduleRepositoryInterface;
import bloodmatch.domain.roles.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetBloodCenterScheduleUseCase {

  private final BloodCenterRepositoryInterface bloodCenterRepository;
  private final BloodCenterScheduleRepositoryInterface scheduleRepository;

  public GetBloodCenterScheduleUseCase(
      BloodCenterRepositoryInterface bloodCenterRepository,
      BloodCenterScheduleRepositoryInterface scheduleRepository) {
    this.bloodCenterRepository = bloodCenterRepository;
    this.scheduleRepository = scheduleRepository;
  }

  public UpsertBloodCenterScheduleUseCase.Output execute(Input input) {
    if (input == null) {
      throw new ValidationException("Request cannot be null");
    }

    DomainID organizationId = DomainIdParser.parse(input.organizationId(), "organizationId");
    PartyOwnership.requireSameParty(organizationId, input.actorPartyId());
    bloodCenterRepository.findByPartyId(organizationId)
        .orElseThrow(() -> new NotFoundException("Blood center role not found"));

    BloodCenterSchedule schedule = scheduleRepository.findByOrganizationId(organizationId)
        .orElse(null);
    if (schedule == null) {
      return new UpsertBloodCenterScheduleUseCase.Output(organizationId.getValue().toString(), List.of(), List.of());
    }
    return UpsertBloodCenterScheduleUseCase.Output.from(schedule);
  }

  public record Input(String organizationId, String actorPartyId) {
  }
}
