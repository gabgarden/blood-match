package bloodmatch.infra.persistence.repository;

import bloodmatch.domain.bloodcenter.schedule.BloodCenterSchedule;
import bloodmatch.domain.bloodcenter.schedule.BloodCenterScheduleRepositoryInterface;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.infra.persistence.repository.mongo.BloodCenterScheduleMongoRepository;
import bloodmatch.infra.persistence.schema.BloodCenterScheduleSchema;
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
    mongoRepository.save(new BloodCenterScheduleSchema(schedule));
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
