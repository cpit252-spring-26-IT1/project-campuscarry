package certifiedcarry_api.user.service;

import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.api.UpdateUserRequest;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.user.repo.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UserService {

  private static final Sort USERS_SORT = Sort.by(Sort.Direction.ASC, "id");
  private static final String ADMIN_ROLE_REQUIRED_MESSAGE = "Admin role is required.";
  private static final String BACKEND_USER_REQUIRED_MESSAGE =
      "Authenticated user is not linked to backend user data.";

  private final UserRepository userRepository;
  private final UserRegistrationService userRegistrationService;
  private final UserMutationService userMutationService;

  public UserService(
      UserRepository userRepository,
      UserRegistrationService userRegistrationService,
      UserMutationService userMutationService) {
    this.userRepository = userRepository;
    this.userRegistrationService = userRegistrationService;
    this.userMutationService = userMutationService;
  }

  public List<UserResponse> getUsers(String id, UserRole role, UserStatus status) {
    if (id != null && !id.isBlank()) {
      Long parsedId = UserFieldNormalizer.parseUserId(valueOrTrim(id), "id must be a numeric string");
      return userRepository.findById(parsedId)
          .filter(user -> role == null || user.getRole() == role)
          .filter(user -> status == null || user.getStatus() == status)
          .map(UserResponseMapper::toResponse)
          .stream()
          .toList();
    }

    List<UserEntity> users;
    if (role != null && status != null) {
      users = userRepository.findAllByRoleAndStatus(role, status, USERS_SORT);
    } else if (role != null) {
      users = userRepository.findAllByRole(role, USERS_SORT);
    } else if (status != null) {
      users = userRepository.findAllByStatus(status, USERS_SORT);
    } else {
      users = userRepository.findAll(USERS_SORT);
    }

    return users.stream().map(UserResponseMapper::toResponse).toList();
  }

  public List<UserResponse> getUsersForActor(
      String id,
      UserRole role,
      UserStatus status,
      Long actorUserId,
      boolean isAdmin) {
    if (requiresAdminAccess(role, status) && !isAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ADMIN_ROLE_REQUIRED_MESSAGE);
    }

    List<UserResponse> users = getUsers(id, role, status);
    if (isAdmin) {
      return users;
    }

    if (actorUserId == null) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, BACKEND_USER_REQUIRED_MESSAGE);
    }

    return users.stream()
        .map(user -> UserResponseMapper.sanitizeForNonAdmin(user, actorUserId))
        .toList();
  }

  @Transactional
  public UserResponse createUser(
      CreateUserRequest request,
      String firebaseUid,
      String firebaseEmail,
      Boolean firebaseEmailVerified) {
    return userRegistrationService.createUser(
        request, firebaseUid, firebaseEmail, firebaseEmailVerified);
  }

  @Transactional
  public UserResponse patchUser(String userId, UpdateUserRequest request) {
    return userMutationService.patchUser(userId, request);
  }

  public RecruiterDmOpenness getRecruiterDmOpenness(long actorUserId) {
    return userMutationService.getRecruiterDmOpenness(actorUserId);
  }

  @Transactional
  public RecruiterDmOpenness updateRecruiterDmOpenness(
      long actorUserId, RecruiterDmOpenness recruiterDmOpenness) {
    return userMutationService.updateRecruiterDmOpenness(actorUserId, recruiterDmOpenness);
  }

  @Transactional
  public void deleteUser(String userId) {
    userMutationService.deleteUser(userId);
  }

  private boolean requiresAdminAccess(UserRole role, UserStatus status) {
    return role == UserRole.RECRUITER || role == UserRole.ADMIN || status != null;
  }

  private String valueOrTrim(String value) {
    return value == null ? null : value.trim();
  }
}
