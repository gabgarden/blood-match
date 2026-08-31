package bloodmatch.infra.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  private String secret;
  private long expirationMs;
  private String issuer;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getExpirationMs() {
    return expirationMs;
  }

  public void setExpirationMs(long expirationMs) {
    this.expirationMs = expirationMs;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  @PostConstruct
  public void validate() {
    if (secret == null || secret.isBlank())
      throw new IllegalStateException("JWT secret is required. Configure JWT_SECRET environment variable.");
    if (secret.length() < 32)
      throw new IllegalStateException("JWT secret must have at least 32 characters.");
    if (expirationMs <= 0)
      throw new IllegalStateException("JWT expiration must be greater than zero.");
    if (issuer == null || issuer.isBlank())
      throw new IllegalStateException("JWT issuer cannot be blank.");
  }
}
