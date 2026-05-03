package certifiedcarry_api.user.factory;

import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;

public interface UserCreationFactory {

  UserRole supportedRole();

  UserEntity create(CreateUserRequest request, PasswordEncoder passwordEncoder);
}
