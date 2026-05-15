package certifiedcarry_api.security;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import certifiedcarry_api.config.SecurityConfig;
import certifiedcarry_api.leaderboard.api.LeaderboardController;
import certifiedcarry_api.leaderboard.service.LeaderboardService;
import certifiedcarry_api.profile.api.PlayerProfileController;
import certifiedcarry_api.profile.service.PlayerProfileService;
import certifiedcarry_api.queue.api.PendingRankController;
import certifiedcarry_api.queue.api.PendingRecruiterController;
import certifiedcarry_api.queue.service.PendingQueueService;
import certifiedcarry_api.support.UserResponseTestBuilder;
import certifiedcarry_api.user.api.UserController;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(
    controllers = {
      UserController.class,
      PlayerProfileController.class,
      LeaderboardController.class,
      PendingRankController.class,
      PendingRecruiterController.class
    })
@Import(SecurityConfig.class)
class ApiAuthorizationHardeningWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;

  @MockitoBean private PlayerProfileService playerProfileService;

  @MockitoBean private LeaderboardService leaderboardService;

  @MockitoBean private PendingQueueService pendingQueueService;

  private static final String ROLE_FIREBASE = "FIREBASE_AUTHENTICATED";
  private static final String ROLE_ADMIN = "ADMIN";
  private static final String ROLE_PLAYER = "PLAYER";
  private static final String ROLE_RECRUITER = "RECRUITER";
  private static final String ATTR_BACKEND_USER_ID = "backendUserId";
  private static final String ATTR_BACKEND_USER_ROLE = "backendUserRole";
  private static final String USERS_ENDPOINT = "/users";
  private static final String LEADERBOARD_ENDPOINT = "/leaderboard";
  private static final String PENDING_RECRUITERS_ENDPOINT = "/pending_recruiters";
  private static final String PENDING_RANKS_ENDPOINT = "/pending_ranks";

  @Test
  void unauthenticatedUsersReadIsRejected() throws Exception {
    mockMvc.perform(get(USERS_ENDPOINT)).andExpect(status().isForbidden());
    verify(userService, never()).getUsers(null, null, null);
  }

  @Test
  void nonAdminMissingBackendUserIdIsForbidden() throws Exception {
    when(userService.getUsers(null, null, null)).thenReturn(List.of());

    mockMvc
        .perform(get(USERS_ENDPOINT).with(user("player").roles(ROLE_FIREBASE)))
        .andExpect(status().isForbidden());
  }

  @Test
  void nonAdminNonNumericBackendUserIdIsBadRequest() throws Exception {
    when(userService.getUsers(null, null, null)).thenReturn(List.of());

    mockMvc
        .perform(
            get(USERS_ENDPOINT)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "not-a-number"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminMissingBackendUserLinkIsForbidden() throws Exception {
    mockMvc
        .perform(get(LEADERBOARD_ENDPOINT)
            .with(user("admin").roles(ROLE_FIREBASE, ROLE_ADMIN)))
        .andExpect(status().isForbidden());

    verify(leaderboardService, never())
        .getLeaderboardEntriesForActor(anyLong(), anyBoolean());
  }

  @Test
  void nonAdminUsersReadMasksSensitiveFieldsForOtherAccounts() throws Exception {
    UserResponse self =
        UserResponseTestBuilder.aPlayer()
            .withId("13")
            .withFullName("Player Self")
            .withUsername("self_tag")
            .withPersonalEmail("self@example.local")
            .withEmail(null)
            .withDeclineReason("")
            .build();

    UserResponse other =
        UserResponseTestBuilder.aPlayer()
            .withId("14")
            .withFullName("Other Player")
            .withUsername("other_tag")
            .withPersonalEmail("other@example.local")
            .withEmail(null)
            .withDeclineReason("hidden")
            .build();

    UserResponse sanitizedOther = new UserResponse(
        other.id(),
        other.fullName(),
        other.username(),
        null,
        null,
        other.organizationName(),
        other.role(),
        other.status(),
        null,
        null,
        other.role() == UserRole.RECRUITER ? other.recruiterDmOpenness() : null,
        other.updatedAt(),
        null,
        null,
        null);

    when(userService.getUsersForActor(isNull(), isNull(), isNull(), eq(13L), eq(false)))
        .thenReturn(List.of(self, sanitizedOther));

    mockMvc
        .perform(
            get(USERS_ENDPOINT)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "13"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value("13"))
        .andExpect(jsonPath("$[0].personalEmail").value("self@example.local"))
        .andExpect(jsonPath("$[1].id").value("14"))
        .andExpect(jsonPath("$[1].personalEmail").doesNotExist())
        .andExpect(jsonPath("$[1].declineReason").doesNotExist())
        .andExpect(jsonPath("$[1].termsVersionAccepted").doesNotExist());
  }

  @Test
  void nonAdminCannotCreateLeaderboardEntries() throws Exception {
    Map<String, Object> payload =
        Map.of(
            "userId", "13",
            "username", "self_tag",
            "game", "Valorant",
            "rank", "Immortal");

    when(leaderboardService.createLeaderboardEntryForActor(anyMap(), eq(13L), eq(false)))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

    mockMvc
        .perform(
            post(LEADERBOARD_ENDPOINT)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "13")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isForbidden());

    verify(leaderboardService)
        .createLeaderboardEntryForActor(anyMap(), eq(13L), eq(false));
  }

  @Test
  void nonAdminCanDeleteOwnLeaderboardEntryOnly() throws Exception {
    String entryId = "77";
    doNothing().when(leaderboardService)
        .deleteLeaderboardEntryForActor(entryId, 13L, false);

    mockMvc
        .perform(
            delete(LEADERBOARD_ENDPOINT + "/{leaderboardEntryId}", entryId)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "13"))
        .andExpect(status().isNoContent());

    verify(leaderboardService).deleteLeaderboardEntryForActor(entryId, 13L, false);
  }

  @Test
  void nonAdminCannotDeleteAnotherLeaderboardEntry() throws Exception {
    String entryId = "88";
    doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"))
        .when(leaderboardService)
        .deleteLeaderboardEntryForActor(entryId, 13L, false);

    mockMvc
        .perform(
            delete(LEADERBOARD_ENDPOINT + "/{leaderboardEntryId}", entryId)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "13"))
        .andExpect(status().isForbidden());

    verify(leaderboardService).deleteLeaderboardEntryForActor(entryId, 13L, false);
  }

  @Test
  void nonAdminProfilePatchDelegatesToService() throws Exception {
    String profileId = "9";

    Map<String, Object> serviceResponse = new LinkedHashMap<>();
    serviceResponse.put("id", profileId);
    serviceResponse.put("userId", "13");
    serviceResponse.put("bio", "updated");
    when(playerProfileService.patchPlayerProfileForActor(eq(profileId), anyMap(), eq(13L), eq(false)))
        .thenReturn(serviceResponse);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("userId", "13");
    payload.put("bio", "updated");
    payload.put("rankVerificationStatus", "APPROVED");
    payload.put("isVerified", true);
    payload.put("declineReason", "force");

    mockMvc
        .perform(
            patch("/player_profiles/{profileId}", profileId)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "13")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(profileId));

    verify(playerProfileService)
        .patchPlayerProfileForActor(eq(profileId), anyMap(), eq(13L), eq(false));
  }

  @Test
  void recruiterCanCreateOwnPendingRecruiterRecordButNotOthers() throws Exception {
    Map<String, Object> ownPayload =
        new LinkedHashMap<>(
            Map.of(
                "userId", "21",
                "fullName", "Recruiter",
                "email", "recruiter@example.local",
                "organizationName", "Org",
                "legalConsentAcceptedAt", "2026-04-04T00:00:00Z",
                "legalConsentLocale", "en",
                "termsVersionAccepted", "cc-terms-2026-04-04",
                "privacyVersionAccepted", "cc-privacy-2026-04-04"));

    when(pendingQueueService.createPendingRecruiterForActor(
        anyMap(), eq(21L), eq(ROLE_RECRUITER), eq(false)))
        .thenReturn(Map.of("id", "501", "userId", "21"))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

    mockMvc
        .perform(
            post(PENDING_RECRUITERS_ENDPOINT)
                .with(user("recruiter").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "21")
                .requestAttr(ATTR_BACKEND_USER_ROLE, ROLE_RECRUITER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ownPayload)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value("21"));

    verify(pendingQueueService)
        .createPendingRecruiterForActor(anyMap(), eq(21L), eq(ROLE_RECRUITER), eq(false));

    Map<String, Object> otherPayload =
        Map.of(
            "userId", "22",
            "fullName", "Recruiter",
            "email", "recruiter@example.local",
            "organizationName", "Org",
            "legalConsentAcceptedAt", "2026-04-04T00:00:00Z",
            "legalConsentLocale", "en",
            "termsVersionAccepted", "cc-terms-2026-04-04",
            "privacyVersionAccepted", "cc-privacy-2026-04-04");

    mockMvc
        .perform(
            post(PENDING_RECRUITERS_ENDPOINT)
                .with(user("recruiter").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "21")
                .requestAttr(ATTR_BACKEND_USER_ROLE, ROLE_RECRUITER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(otherPayload)))
        .andExpect(status().isForbidden());

    verify(pendingQueueService, times(2))
        .createPendingRecruiterForActor(anyMap(), eq(21L), eq(ROLE_RECRUITER), eq(false));
  }

  @Test
  void nonRecruiterCannotCreatePendingRecruiterRecord() throws Exception {
    Map<String, Object> payload =
        Map.of(
            "userId", "13",
            "fullName", "Player",
            "email", "player@example.local",
            "organizationName", "NoOrg",
            "legalConsentAcceptedAt", "2026-04-04T00:00:00Z",
            "legalConsentLocale", "en",
            "termsVersionAccepted", "cc-terms-2026-04-04",
            "privacyVersionAccepted", "cc-privacy-2026-04-04");

    when(pendingQueueService.createPendingRecruiterForActor(
        anyMap(), eq(13L), eq(ROLE_PLAYER), eq(false)))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

    mockMvc
        .perform(
            post(PENDING_RECRUITERS_ENDPOINT)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "13")
                .requestAttr(ATTR_BACKEND_USER_ROLE, ROLE_PLAYER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isForbidden());

    verify(pendingQueueService)
        .createPendingRecruiterForActor(anyMap(), eq(13L), eq(ROLE_PLAYER), eq(false));
  }

  @Test
  void missingBackendUserRoleCannotCreatePendingRecruiterRecord() throws Exception {
    Map<String, Object> payload =
        Map.of(
            "userId", "21",
            "fullName", "Recruiter",
            "email", "recruiter@example.local",
            "organizationName", "Org",
            "legalConsentAcceptedAt", "2026-04-04T00:00:00Z",
            "legalConsentLocale", "en",
            "termsVersionAccepted", "cc-terms-2026-04-04",
            "privacyVersionAccepted", "cc-privacy-2026-04-04");

    when(pendingQueueService.createPendingRecruiterForActor(
        anyMap(), eq(21L), eq(""), eq(false)))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

    mockMvc
        .perform(
            post(PENDING_RECRUITERS_ENDPOINT)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "21")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isForbidden());

    verify(pendingQueueService)
        .createPendingRecruiterForActor(anyMap(), eq(21L), eq(""), eq(false));
  }

  @Test
  void spoofedRecruiterRoleStillCannotCreateRecordForAnotherUser() throws Exception {
    Map<String, Object> payload =
        Map.of(
            "userId", "999",
            "fullName", "Recruiter",
            "email", "recruiter@example.local",
            "organizationName", "Org",
            "legalConsentAcceptedAt", "2026-04-04T00:00:00Z",
            "legalConsentLocale", "en",
            "termsVersionAccepted", "cc-terms-2026-04-04",
            "privacyVersionAccepted", "cc-privacy-2026-04-04");

    when(pendingQueueService.createPendingRecruiterForActor(
        anyMap(), eq(21L), eq(ROLE_RECRUITER), eq(false)))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "forbidden"));

    mockMvc
        .perform(
            post(PENDING_RECRUITERS_ENDPOINT)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "21")
                .requestAttr(ATTR_BACKEND_USER_ROLE, ROLE_RECRUITER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isForbidden());

    verify(pendingQueueService)
        .createPendingRecruiterForActor(anyMap(), eq(21L), eq(ROLE_RECRUITER), eq(false));
  }

  @Test
  void declinedUserIsBlockedBySevenDayRankCooldown() throws Exception {
    when(pendingQueueService.createPendingRankForActor(anyMap(), eq(13L), eq(false)))
        .thenThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "cooldown"));

    Map<String, Object> payload =
        Map.of(
            "userId", "13",
            "username", "self_tag",
            "fullName", "Player Self",
            "game", "Valorant",
            "claimedRank", "Immortal",
            "proofImage", "proof");

    mockMvc
        .perform(
            post(PENDING_RANKS_ENDPOINT)
                .with(user("player").roles(ROLE_FIREBASE))
                .requestAttr(ATTR_BACKEND_USER_ID, "13")
                .requestAttr(ATTR_BACKEND_USER_ROLE, ROLE_PLAYER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
        .andExpect(status().isTooManyRequests());

    verify(pendingQueueService).createPendingRankForActor(anyMap(), eq(13L), eq(false));
  }
}