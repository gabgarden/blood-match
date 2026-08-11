package bloodmatch.infra.persistence.schema;

import bloodmatch.domain.security.SecurityRole;
import bloodmatch.domain.security.UserAccount;
import bloodmatch.domain.shared.valueObjects.DomainID;
import bloodmatch.domain.shared.valueObjects.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Document(collection = "user_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountSchema {

  @Id
  private String id;
  @Indexed(unique = true)
  private String partyId;
  @Indexed(unique = true)
  private String email;
  private String passwordHash;
  private Set<String> roles;
  private Boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public UserAccountSchema(UserAccount userAccount) {
    if (userAccount == null)
      throw new IllegalArgumentException("UserAccount cannot be null");

    this.id = userAccount.getId().getValue().toString();
    this.partyId = userAccount.getPartyId().getValue().toString();
    this.email = userAccount.getEmail().getValue();
    this.passwordHash = userAccount.getPasswordHash();
    this.roles = userAccount.getRoles().stream()
        .map(Enum::name)
        .collect(Collectors.toSet());
    this.enabled = userAccount.isEnabled();
    this.createdAt = userAccount.getCreatedAt();
    this.updatedAt = userAccount.getUpdatedAt();
  }

  public UserAccount toDomain() {
    Set<SecurityRole> parsedRoles = new HashSet<>();
    if (roles != null) {
      parsedRoles = roles.stream()
          .map(SecurityRole::valueOf)
          .collect(Collectors.toSet());
    }

    return UserAccount.rehydrate(
        new DomainID(UUID.fromString(this.id)),
        new DomainID(UUID.fromString(this.partyId)),
        new Email(this.email),
        this.passwordHash,
        parsedRoles,
        Boolean.TRUE.equals(this.enabled),
        this.createdAt,
        this.updatedAt);
  }
}
