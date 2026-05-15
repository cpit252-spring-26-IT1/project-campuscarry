package certifiedcarry_api.user.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

class FirebaseAccountDeletionQueueServiceTest {

  @Test
  void deleteOrQueueSkipsBlankIdentifiersQueuesUnexpectedFailuresAndFallsBackToEmail() throws Exception {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    FirebaseAccountDeletionQueueService service = new FirebaseAccountDeletionQueueService(jdbcTemplate);
    ReflectionTestUtils.setField(service, "retryBaseDelaySeconds", 30);
    ReflectionTestUtils.setField(service, "retryMaxDelaySeconds", 3600);

    service.deleteOrQueue(7L, "   ", "  ");
    verifyNoInteractions(jdbcTemplate);

    FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    FirebaseApp firebaseApp = mock(FirebaseApp.class);

    try (MockedStatic<FirebaseApp> firebaseAppStatic = Mockito.mockStatic(FirebaseApp.class);
        MockedStatic<FirebaseAuth> firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth.class)) {
      firebaseAppStatic.when(FirebaseApp::getApps).thenReturn(List.of(firebaseApp));
      firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

      doThrow(new IllegalStateException("boom")).when(firebaseAuth).deleteUser("uid-1");
      service.deleteOrQueue(7L, " uid-1 ", " player@example.com ");
      verify(jdbcTemplate)
          .update(
              anyString(),
              eq(7L),
              eq("uid-1"),
              eq("player@example.com"),
              eq(1),
              eq("boom"),
              eq(30L));

      FirebaseAuthException missingUid = mock(FirebaseAuthException.class);
      when(missingUid.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_NOT_FOUND);
      when(missingUid.getMessage()).thenReturn("missing uid");
      UserRecord userRecord = mock(UserRecord.class);
      when(userRecord.getUid()).thenReturn("resolved-uid");

      doThrow(missingUid).when(firebaseAuth).deleteUser("uid-2");
      when(firebaseAuth.getUserByEmail("fallback@example.com")).thenReturn(userRecord);

      assertDoesNotThrow(() -> service.deleteOrQueue(8L, "uid-2", "fallback@example.com"));
      verify(firebaseAuth).deleteUser("resolved-uid");
    }
  }

  @Test
  void retryQueuedDeletionsSkipsWithoutFirebaseAndSchedulesRetriesForFailures() throws Exception {
    JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    FirebaseAccountDeletionQueueService service = new FirebaseAccountDeletionQueueService(jdbcTemplate);
    ReflectionTestUtils.setField(service, "retryBatchSize", 2);
    ReflectionTestUtils.setField(service, "retryBaseDelaySeconds", 30);
    ReflectionTestUtils.setField(service, "retryMaxDelaySeconds", 3600);

    try (MockedStatic<FirebaseApp> firebaseAppStatic = Mockito.mockStatic(FirebaseApp.class)) {
      firebaseAppStatic.when(FirebaseApp::getApps).thenReturn(List.of());
      service.retryQueuedDeletions();
      verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), anyInt());
    }

    FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    FirebaseApp firebaseApp = mock(FirebaseApp.class);
    FirebaseAuthException notFound = mock(FirebaseAuthException.class);
    when(notFound.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_NOT_FOUND);
    when(notFound.getMessage()).thenReturn("missing");
    FirebaseAuthException transientFailure = mock(FirebaseAuthException.class);
    when(transientFailure.getAuthErrorCode()).thenReturn(AuthErrorCode.USER_DISABLED);
    when(transientFailure.getMessage()).thenReturn("temporary");

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(
                  mapper.mapRow(mockQueueResultSet(1L, 7L, "uid-1", null, 1), 0),
                  mapper.mapRow(mockQueueResultSet(2L, 8L, "uid-2", "b@example.com", 1), 1));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(2));

    try (MockedStatic<FirebaseApp> firebaseAppStatic = Mockito.mockStatic(FirebaseApp.class);
        MockedStatic<FirebaseAuth> firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth.class)) {
      firebaseAppStatic.when(FirebaseApp::getApps).thenReturn(List.of(firebaseApp));
      firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

      doThrow(notFound).when(firebaseAuth).deleteUser("uid-1");
      doThrow(transientFailure).when(firebaseAuth).deleteUser("uid-2");

      service.retryQueuedDeletions();

      verify(jdbcTemplate).update("DELETE FROM firebase_account_deletion_queue WHERE id = ?", 1L);
      verify(jdbcTemplate)
          .update(
              anyString(),
              eq(2),
              eq("temporary"),
              eq(60L),
              eq(2L));
    }
  }

  private ResultSet mockQueueResultSet(long id, Long backendUserId, String firebaseUid, String email, int attempts)
      throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getLong("id")).thenReturn(id);
    when(resultSet.getObject("backend_user_id", Long.class)).thenReturn(backendUserId);
    when(resultSet.getString("firebase_uid")).thenReturn(firebaseUid);
    when(resultSet.getString("email")).thenReturn(email);
    when(resultSet.getInt("attempts")).thenReturn(attempts);
    return resultSet;
  }
}
