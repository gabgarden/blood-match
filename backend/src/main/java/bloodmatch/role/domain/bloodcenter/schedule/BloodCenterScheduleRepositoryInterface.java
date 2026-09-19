package bloodmatch.role.domain.bloodcenter.schedule;

import bloodmatch.shared.domain.valueObjects.DomainID;

import java.util.Optional;

public interface BloodCenterScheduleRepositoryInterface {

  void save(BloodCenterSchedule schedule);

  Optional<BloodCenterSchedule> findByOrganizationId(DomainID organizationId);
}
