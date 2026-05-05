package certifiedcarry_api.user.factory;

import certifiedcarry_api.user.model.UserRole;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UserFactorySelector {

  private final Map<UserRole, UserCreationFactory> factoryByRole = new EnumMap<>(UserRole.class);

  public UserFactorySelector(List<UserCreationFactory> factories) {
    for (UserCreationFactory factory : factories) {
      factoryByRole.put(factory.supportedRole(), factory);
    }
  }

  public UserCreationFactory getFactory(UserRole role) {
    return factoryByRole.get(role);
  }
}
