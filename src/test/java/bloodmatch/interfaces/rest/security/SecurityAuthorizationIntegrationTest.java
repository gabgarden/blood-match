package bloodmatch.interfaces.rest.security;

import bloodmatch.application.usecase.donationrequest.GetDonationRequestsByPartyIdUseCase;
import bloodmatch.application.usecase.donationrequest.recommendations.GetRecommendedRequestsUseCase;
import bloodmatch.infra.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private JwtTokenProvider jwtTokenProvider;

  @MockitoBean
  private GetRecommendedRequestsUseCase getRecommendedRequestsUseCase;

  @MockitoBean
  private GetDonationRequestsByPartyIdUseCase getDonationRequestsByPartyIdUseCase;

  @Test
  void shouldReturn401WhenTokenIsMissing() throws Exception {
    mockMvc.perform(get("/requests/recommendations")
            .param("personId", UUID.randomUUID().toString()))
        .andExpect(status().isUnauthorized());

    verify(getRecommendedRequestsUseCase, never()).execute(any(GetRecommendedRequestsUseCase.Input.class));
  }

  @Test
  void shouldReturn403WhenTokenRoleIsNotAllowed() throws Exception {
    String token = "requester-token";

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    when(jwtTokenProvider.extractRoles(token)).thenReturn(List.of("REQUESTER"));
    when(jwtTokenProvider.extractUserId(token)).thenReturn(UUID.randomUUID().toString());
    when(jwtTokenProvider.extractPartyId(token)).thenReturn(UUID.randomUUID().toString());

    mockMvc.perform(get("/requests/recommendations")
            .param("personId", UUID.randomUUID().toString())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());

    verify(getRecommendedRequestsUseCase, never()).execute(any(GetRecommendedRequestsUseCase.Input.class));
  }

  @Test
  void shouldReturn200WhenTokenHasAllowedRole() throws Exception {
    String token = "donor-token";

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    when(jwtTokenProvider.extractRoles(token)).thenReturn(List.of("DONOR"));
    when(jwtTokenProvider.extractUserId(token)).thenReturn(UUID.randomUUID().toString());
    when(jwtTokenProvider.extractPartyId(token)).thenReturn(UUID.randomUUID().toString());
    when(getRecommendedRequestsUseCase.execute(any(GetRecommendedRequestsUseCase.Input.class))).thenReturn(List.of());

    mockMvc.perform(get("/requests/recommendations")
            .param("personId", UUID.randomUUID().toString())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void shouldReturn401WhenTokenIsMissingForUserDonationRequests() throws Exception {
    mockMvc.perform(get("/donation-requests/{partyId}", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());

    verify(getDonationRequestsByPartyIdUseCase, never()).execute(any(GetDonationRequestsByPartyIdUseCase.Input.class));
  }

  @Test
  void shouldReturn403WhenTokenRoleIsNotAllowedForUserDonationRequests() throws Exception {
    String token = "donor-token";

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    when(jwtTokenProvider.extractRoles(token)).thenReturn(List.of("DONOR"));
    when(jwtTokenProvider.extractUserId(token)).thenReturn(UUID.randomUUID().toString());
    when(jwtTokenProvider.extractPartyId(token)).thenReturn(UUID.randomUUID().toString());

    mockMvc.perform(get("/donation-requests/{partyId}", UUID.randomUUID())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());

    verify(getDonationRequestsByPartyIdUseCase, never()).execute(any(GetDonationRequestsByPartyIdUseCase.Input.class));
  }

  @Test
  void shouldReturn200WhenTokenHasAllowedRoleForUserDonationRequests() throws Exception {
    String token = "requester-token";

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    when(jwtTokenProvider.extractRoles(token)).thenReturn(List.of("REQUESTER"));
    when(jwtTokenProvider.extractUserId(token)).thenReturn(UUID.randomUUID().toString());
    String partyId = UUID.randomUUID().toString();
    when(jwtTokenProvider.extractPartyId(token)).thenReturn(partyId);
    when(getDonationRequestsByPartyIdUseCase.execute(any(GetDonationRequestsByPartyIdUseCase.Input.class)))
        .thenReturn(List.of());

    mockMvc.perform(get("/donation-requests/{partyId}", partyId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void shouldReturn403WhenRequesterReadsAnotherPartyDonationRequests() throws Exception {
    String token = "requester-token";

    when(jwtTokenProvider.validateToken(token)).thenReturn(true);
    when(jwtTokenProvider.extractRoles(token)).thenReturn(List.of("REQUESTER"));
    when(jwtTokenProvider.extractUserId(token)).thenReturn(UUID.randomUUID().toString());
    when(jwtTokenProvider.extractPartyId(token)).thenReturn(UUID.randomUUID().toString());

    mockMvc.perform(get("/donation-requests/{partyId}", UUID.randomUUID())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());

    verify(getDonationRequestsByPartyIdUseCase, never()).execute(any(GetDonationRequestsByPartyIdUseCase.Input.class));
  }
}
