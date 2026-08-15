package bloodmatch.domain.bloodcenter.schedule;

import bloodmatch.domain.shared.valueObjects.DomainID;

import java.util.Optional;

public interface BloodCenterScheduleRepositoryInterface {

  void save(BloodCenterSchedule schedule);

  Optional<BloodCenterSchedule> findByOrganizationId(DomainID organizationId);
}
