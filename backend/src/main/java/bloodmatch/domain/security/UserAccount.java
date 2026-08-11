package bloodmatch.domain.security;

import bloodmatch.domain.shared.entity.DomainObject;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserAccount extends DomainObject {

  private DomainID partyId;
  private Email email;
  private String passwordHash;
  private Set<SecurityRole> roles;
  private boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public UserAccount(
      DomainID partyId,
      Email email,
      String passwordHash,
      Set<SecurityRole> roles) {

    this.id = DomainID.generate();
    this.partyId = requirePartyId(partyId);
    this.email = requireEmail(email);
    this.passwordHash = requirePasswordHash(passwordHash);
    this.roles = copyRoles(roles);
    this.enabled = true;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
  }

  private UserAccount(
      DomainID partyId,
      Email email,
      String passwordHash,
      Set<SecurityRole> roles,
      boolean enabled,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    this.partyId = requirePartyId(partyId);
    this.email = requireEmail(email);
    this.passwordHash = requirePasswordHash(passwordHash);
    this.roles = copyRoles(roles);
    this.enabled = enabled;
    this.createdAt = requireTimestamp(createdAt, "createdAt");
    this.updatedAt = requireTimestamp(updatedAt, "updatedAt");
  }

  public static UserAccount rehydrate(
      DomainID id,
      DomainID partyId,
      Email email,
      String passwordHash,
      Set<SecurityRole> roles,
      boolean enabled,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    if (id == null)
      throw new IllegalArgumentException("User id cannot be null");

    UserAccount userAccount = new UserAccount(
        partyId,
        email,
        passwordHash,
        roles,
        enabled,
        createdAt,
        updatedAt);
    userAccount.setId(id);
    return userAccount;
  }

  public DomainID getPartyId() {
    return partyId;
  }

  public Email getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Set<SecurityRole> getRoles() {
    return Collections.unmodifiableSet(roles);
  }

  public boolean isEnabled() {
    return enabled;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void updatePasswordHash(String newPasswordHash) {
    this.passwordHash = requirePasswordHash(newPasswordHash);
    this.updatedAt = LocalDateTime.now();
  }

  public void updateRoles(Set<SecurityRole> newRoles) {
    this.roles = copyRoles(newRoles);
    this.updatedAt = LocalDateTime.now();
  }

  public void disable() {
    this.enabled = false;
    this.updatedAt = LocalDateTime.now();
  }

  public void enable() {
    this.enabled = true;
    this.updatedAt = LocalDateTime.now();
  }

  private static DomainID requirePartyId(DomainID partyId) {
    if (partyId == null)
      throw new IllegalArgumentException("Party id cannot be null");
    return partyId;
  }

  private static Email requireEmail(Email email) {
    if (email == null)
      throw new IllegalArgumentException("Email cannot be null");
    return email;
  }

  private static String requirePasswordHash(String passwordHash) {
    if (passwordHash == null || passwordHash.isBlank())
      throw new IllegalArgumentException("Password hash cannot be blank");
    return passwordHash;
  }

  private static Set<SecurityRole> copyRoles(Set<SecurityRole> roles) {
    if (roles == null)
      return new HashSet<>();

    Set<SecurityRole> normalized = new HashSet<>(roles);
    if (normalized.contains(null))
      throw new IllegalArgumentException("Roles cannot contain null");

    return normalized;
  }

  private static LocalDateTime requireTimestamp(LocalDateTime value, String fieldName) {
    if (value == null)
      throw new IllegalArgumentException(fieldName + " cannot be null");
    return value;
  }
}
