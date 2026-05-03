package certifiedcarry_api.user.repo;

import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

  List<UserEntity> findAllByRole(UserRole role, Sort sort);

  List<UserEntity> findAllByStatus(UserStatus status, Sort sort);

  List<UserEntity> findAllByRoleAndStatus(UserRole role, UserStatus status, Sort sort);

  boolean existsByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

  boolean existsByPersonalEmailIgnoreCase(String personalEmail);

  boolean existsByPersonalEmailIgnoreCaseAndIdNot(String personalEmail, Long id);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

  Optional<UserEntity> findByEmailIgnoreCase(String email);

  Optional<UserEntity> findByPersonalEmailIgnoreCase(String personalEmail);

  Optional<UserEntity> findByFirebaseUid(String firebaseUid);
}
