package bloodmatch.role.infrastructure;
import bloodmatch.shared.infrastructure.persistence.repository.OptimisticConcurrency;

import bloodmatch.role.domain.bloodcenter.inventory.BloodCenterInventory;
import bloodmatch.role.domain.bloodcenter.inventory.BloodCenterInventoryRepositoryInterface;
import bloodmatch.shared.domain.valueObjects.DomainID;
import bloodmatch.role.infrastructure.mongo.BloodCenterInventoryMongoRepository;
import bloodmatch.role.infrastructure.schema.BloodCenterInventorySchema;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class BloodCenterInventoryRepositoryImpl implements BloodCenterInventoryRepositoryInterface {

  private final BloodCenterInventoryMongoRepository mongoRepository;

  public BloodCenterInventoryRepositoryImpl(BloodCenterInventoryMongoRepository mongoRepository) {
    this.mongoRepository = mongoRepository;
  }

  @Override
  public void save(BloodCenterInventory inventory) {
    if (inventory == null) {
      throw new IllegalArgumentException("Inventory cannot be null");
    }
    OptimisticConcurrency.save(
        () -> mongoRepository.save(new BloodCenterInventorySchema(inventory)),
        "Blood center inventory");
  }

  @Override
  public Optional<BloodCenterInventory> findByOrganizationId(DomainID organizationId) {
    if (organizationId == null) {
      throw new IllegalArgumentException("organizationId cannot be null");
    }
    return mongoRepository.findById(organizationId.getValue().toString())
        .map(BloodCenterInventorySchema::toDomain);
  }

  @Override
  public List<BloodCenterInventory> findAll() {
    return mongoRepository.findAll().stream()
        .map(BloodCenterInventorySchema::toDomain)
        .toList();
  }
}
