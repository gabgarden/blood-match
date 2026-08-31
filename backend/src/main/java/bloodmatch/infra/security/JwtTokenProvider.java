package bloodmatch.infra.security;

import bloodmatch.domain.security.UserAccount;
import bloodmatch.infra.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

  private final JwtProperties jwtProperties;

  public JwtTokenProvider(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
  }

  public String generateAccessToken(UserAccount userAccount) {
    Instant now = Instant.now();
    Instant expiration = now.plusMillis(jwtProperties.getExpirationMs());

    List<String> roleNames = userAccount.getRoles().stream()
        .map(Enum::name)
        .toList();

    return Jwts.builder()
        .subject(userAccount.getId().getValue().toString())
        .issuer(jwtProperties.getIssuer())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .claim("partyId", userAccount.getPartyId().getValue().toString())
        .claim("roles", roleNames)
        .signWith(getSigningKey())
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public String extractUserId(String token) {
    return parseClaims(token).getSubject();
  }

  public String extractPartyId(String token) {
    Object partyId = parseClaims(token).get("partyId");
    if (partyId == null)
      throw new IllegalArgumentException("Token missing partyId claim");
    return partyId.toString();
  }

  public List<String> extractRoles(String token) {
    Object roles = parseClaims(token).get("roles");
    if (roles == null)
      return List.of();
    if (roles instanceof List<?> list)
      return list.stream().map(Object::toString).toList();
    throw new IllegalArgumentException("Invalid roles claim in token");
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .requireIssuer(jwtProperties.getIssuer())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private SecretKey getSigningKey() {
    byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
