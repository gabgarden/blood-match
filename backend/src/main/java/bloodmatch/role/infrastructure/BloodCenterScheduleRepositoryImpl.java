package bloodmatch.role.infrastructure;
import bloodmatch.shared.infrastructure.persistence.repository.OptimisticConcurrency;

import bloodmatch.role.domain.bloodcenter.schedule.BloodCenterSchedule;
import bloodmatch.role.domain.bloodcenter.schedule.BloodCenterScheduleRepositoryInterface;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.role.infrastructure.mongo.BloodCenterScheduleMongoRepository;
import bloodmatch.role.infrastructure.schema.BloodCenterScheduleSchema;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class BloodCenterScheduleRepositoryImpl implements BloodCenterScheduleRepositoryInterface {

  private final BloodCenterScheduleMongoRepository mongoRepository;

  public BloodCenterScheduleRepositoryImpl(BloodCenterScheduleMongoRepository mongoRepository) {
    this.mongoRepository = mongoRepository;
  }

  @Override
  public void save(BloodCenterSchedule schedule) {
    if (schedule == null) {
      throw new IllegalArgumentException("Schedule cannot be null");
    }
    OptimisticConcurrency.save(
        () -> mongoRepository.save(new BloodCenterScheduleSchema(schedule)),
        "Blood center schedule");
  }

  @Override
  public Optional<BloodCenterSchedule> findByOrganizationId(DomainID organizationId) {
    if (organizationId == null) {
      throw new IllegalArgumentException("organizationId cannot be null");
    }
    return mongoRepository.findById(organizationId.getValue().toString())
        .map(BloodCenterScheduleSchema::toDomain);
  }
}
