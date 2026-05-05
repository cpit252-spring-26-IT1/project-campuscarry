package certifiedcarry_api.config.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseAdminConfig {

  @Bean
  @ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
  public FirebaseApp firebaseApp(FirebaseProperties firebaseProperties) throws IOException {
    if (firebaseProperties.getServiceAccountPath() == null
        || firebaseProperties.getServiceAccountPath().isBlank()) {
      throw new IllegalStateException(
          "FIREBASE_SERVICE_ACCOUNT_PATH must be set when firebase.enabled=true");
    }

    if (!FirebaseApp.getApps().isEmpty()) {
      return FirebaseApp.getInstance();
    }

    try (InputStream serviceAccount = new FileInputStream(firebaseProperties.getServiceAccountPath())) {
      GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
      FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder().setCredentials(credentials);

      if (firebaseProperties.getProjectId() != null && !firebaseProperties.getProjectId().isBlank()) {
        optionsBuilder.setProjectId(firebaseProperties.getProjectId().trim());
      }

      return FirebaseApp.initializeApp(optionsBuilder.build());
    }
  }
}
