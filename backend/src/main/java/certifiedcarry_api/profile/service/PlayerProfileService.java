package certifiedcarry_api.profile.service;

import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.shared.SqlErrorMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PlayerProfileService {

  private static final String PROFILE_NOT_FOUND_PREFIX = "Player profile not found for id ";

  private final PlayerProfileRepositoryGateway repositoryGateway;
  private final PlayerProfilePayloadFactory payloadFactory;
  private final PlayerProfileAccessPolicy accessPolicy;

  @Autowired
  public PlayerProfileService(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      @Value("${notifications.rank-validation-validity-days:30}") int rankValidationValidityDays) {
    this(
        new PlayerProfileRepositoryGateway(jdbcTemplate, objectMapper),
        new PlayerProfilePayloadFactory(objectMapper, rankValidationValidityDays),
        new PlayerProfileAccessPolicy());
  }

  PlayerProfileService(
      PlayerProfileRepositoryGateway repositoryGateway,
      PlayerProfilePayloadFactory payloadFactory,
      PlayerProfileAccessPolicy accessPolicy) {
    this.repositoryGateway = repositoryGateway;
    this.payloadFactory = payloadFactory;
    this.accessPolicy = accessPolicy;
  }

  public List<Map<String, Object>> getPlayerProfilesForActor(
      String userId, long actorUserId, boolean isAdmin) {
    List<Map<String, Object>> profiles = getPlayerProfiles(userId);
    if (isAdmin) {
      return profiles;
    }

    return profiles.stream()
        .map(profile -> accessPolicy.sanitizeProfileForActor(profile, actorUserId))
        .toList();
  }

  @Transactional
  public Map<String, Object> createPlayerProfileForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    Map<String, Object> mutableRequest = new LinkedHashMap<>(request);
    if (!isAdmin) {
      accessPolicy.enforcePayloadUserOwnership(mutableRequest, actorUserId);
      accessPolicy.stripAdminVerificationFields(mutableRequest);
    }

    return createPlayerProfile(mutableRequest);
  }

  @Transactional
  public Map<String, Object> patchPlayerProfileForActor(
      String profileId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    Map<String, Object> mutableRequest = new LinkedHashMap<>(request);

    if (!isAdmin) {
      accessPolicy.enforcePayloadUserOwnership(mutableRequest, actorUserId);
      accessPolicy.stripAdminVerificationFields(mutableRequest);

      if (!isProfileOwnedBy(profileId, actorUserId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You can only update your own player profile.");
      }
    }

    return patchPlayerProfile(profileId, mutableRequest);
  }

  public List<Map<String, Object>> getPlayerProfiles(String userId) {
    return repositoryGateway.getPlayerProfiles(userId);
  }

  public boolean isProfileOwnedBy(String profileId, long expectedUserId) {
    return repositoryGateway.isProfileOwnedBy(profileId, expectedUserId);
  }

  @Transactional
  public Map<String, Object> createPlayerProfile(Map<String, Object> request) {
    PlayerProfilePayload payload = payloadFactory.buildCreatePayload(request);

    try {
      Long createdId = repositoryGateway.insertProfile(payload);
      return repositoryGateway.findPlayerProfileById(createdId);
    } catch (DataIntegrityViolationException exception) {
      throw SqlErrorMapper.mapDataIntegrityViolation(
          exception,
          "Player profile already exists for this user.",
          "Invalid player profile payload.",
          HttpErrors::badRequest,
          HttpErrors::conflict);
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getSQLException(), "Invalid player profile payload."));
    }
  }

  @Transactional
  public Map<String, Object> patchPlayerProfile(String profileId, Map<String, Object> request) {
    long parsedId = certifiedcarry_api.shared.HttpRequestParsers.parsePathId(profileId, "profileId");
    PlayerProfileRow existing = repositoryGateway.findPlayerProfileRow(parsedId);
    PlayerProfilePayload payload = payloadFactory.buildPatchPayload(request, existing);

    try {
      int updatedRows = repositoryGateway.updateProfile(parsedId, payload);
      if (updatedRows == 0) {
        throw HttpErrors.notFound(PROFILE_NOT_FOUND_PREFIX + profileId);
      }

      return repositoryGateway.findPlayerProfileById(parsedId);
    } catch (DataIntegrityViolationException exception) {
      throw SqlErrorMapper.mapDataIntegrityViolation(
          exception,
          "Player profile patch conflicts with existing records.",
          "Invalid player profile patch payload.",
          HttpErrors::badRequest,
          HttpErrors::conflict);
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getSQLException(), "Invalid player profile patch payload."));
    }
  }
}
