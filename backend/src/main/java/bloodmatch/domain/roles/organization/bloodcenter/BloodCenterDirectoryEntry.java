package bloodmatch.domain.roles.organization.bloodcenter;

/**
 * Lightweight directory projection for blood-center search results.
 */
public record BloodCenterDirectoryEntry(
    String organizationId,
    String name,
    String city,
    String state) {
}
