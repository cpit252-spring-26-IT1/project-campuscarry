package certifiedcarry_api.config.firebase;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "firebase")
@Getter
@Setter
public class FirebaseProperties {

  private boolean enabled;
  private String projectId;
  private String serviceAccountPath;
}
