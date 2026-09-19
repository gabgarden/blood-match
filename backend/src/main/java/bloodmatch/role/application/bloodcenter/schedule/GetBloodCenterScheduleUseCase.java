package bloodmatch.role.application.bloodcenter.schedule;

import bloodmatch.shared.application.exception.NotFoundException;
import bloodmatch.shared.application.exception.ValidationException;
import bloodmatch.shared.application.shared.DomainIdParser;
import bloodmatch.shared.application.shared.PartyOwnership;
import bloodmatch.role.domain.bloodcenter.schedule.BloodCenterSchedule;
import bloodmatch.role.domain.bloodcenter.schedule.BloodCenterScheduleRepositoryInterface;
import bloodmatch.role.domain.organization.bloodcenter.BloodCenterRepositoryInterface;
import bloodmatch.shared.domain.valueObjects.DomainID;
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
