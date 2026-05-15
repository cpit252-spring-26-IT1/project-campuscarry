package certifiedcarry_api.config.firebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class FirebaseAdminConfigTest {

  @Test
  void requiresServiceAccountPathWhenFirebaseIsEnabled() {
    FirebaseProperties properties = new FirebaseProperties();
    properties.setEnabled(true);

    IllegalStateException failure =
        assertThrows(
            IllegalStateException.class,
            () -> new FirebaseAdminConfig().firebaseApp(properties));

    assertEquals(
        "FIREBASE_SERVICE_ACCOUNT_PATH must be set when firebase.enabled=true",
        failure.getMessage());
  }

  @Test
  void reusesExistingFirebaseAppBeforeReadingCredentials() throws IOException {
    FirebaseProperties properties = new FirebaseProperties();
    properties.setEnabled(true);
    properties.setServiceAccountPath("unused.json");

    FirebaseApp existing = mock(FirebaseApp.class);

    try (MockedStatic<FirebaseApp> firebaseAppMock = Mockito.mockStatic(FirebaseApp.class)) {
      firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of(existing));
      firebaseAppMock.when(FirebaseApp::getInstance).thenReturn(existing);

      assertSame(existing, new FirebaseAdminConfig().firebaseApp(properties));
    }
  }

  @Test
  void initializesFirebaseAppWithTrimmedProjectIdWhenNoInstanceExists() throws IOException {
    FirebaseProperties properties = new FirebaseProperties();
    properties.setEnabled(true);
    properties.setServiceAccountPath(tempServiceAccountPath());
    properties.setProjectId("  certifiedcarry-prod  ");

    FirebaseApp initialized = mock(FirebaseApp.class);
    GoogleCredentials credentials = mock(GoogleCredentials.class);
    AtomicReference<FirebaseOptions> capturedOptions = new AtomicReference<>();

    try (MockedStatic<FirebaseApp> firebaseAppMock = Mockito.mockStatic(FirebaseApp.class);
        MockedStatic<GoogleCredentials> credentialsMock = Mockito.mockStatic(GoogleCredentials.class)) {
      firebaseAppMock.when(FirebaseApp::getApps).thenReturn(List.of());
      firebaseAppMock
          .when(() -> FirebaseApp.initializeApp(any(FirebaseOptions.class)))
          .thenAnswer(
              invocation -> {
                capturedOptions.set(invocation.getArgument(0));
                return initialized;
              });
      credentialsMock.when(() -> GoogleCredentials.fromStream(any())).thenReturn(credentials);

      FirebaseApp app = new FirebaseAdminConfig().firebaseApp(properties);

      assertSame(initialized, app);
      assertEquals("certifiedcarry-prod", capturedOptions.get().getProjectId());
    }
  }

  private String tempServiceAccountPath() throws IOException {
    Path tempFile = Files.createTempFile("firebase-service-account", ".json");
    Files.writeString(tempFile, "{}");
    return tempFile.toString();
  }
}
