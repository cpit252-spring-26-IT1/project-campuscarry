package certifiedcarry_api.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RocketLeagueModesCodecTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void readsSqlArraysAsNormalizedStrings() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    Array sqlArray = mock(Array.class);
    when(resultSet.getArray("rocket_league_modes")).thenReturn(sqlArray);
    when(sqlArray.getArray()).thenReturn(new Object[] {"2v2", null, 1337});

    assertEquals(
        List.of("2v2", "1337"),
        RocketLeagueModesCodec.readTextArray(resultSet, "rocket_league_modes"));
  }

  @Test
  void returnsEmptyWhenSqlArrayIsMissingOrUnsupported() throws SQLException {
    ResultSet missingArrayResultSet = mock(ResultSet.class);
    when(missingArrayResultSet.getArray("rocket_league_modes")).thenReturn(null);
    assertEquals(List.of(), RocketLeagueModesCodec.readTextArray(missingArrayResultSet, "rocket_league_modes"));

    ResultSet unsupportedArrayResultSet = mock(ResultSet.class);
    Array sqlArray = mock(Array.class);
    when(unsupportedArrayResultSet.getArray("rocket_league_modes")).thenReturn(sqlArray);
    when(sqlArray.getArray()).thenReturn(123);
    assertEquals(
        List.of(),
        RocketLeagueModesCodec.readTextArray(unsupportedArrayResultSet, "rocket_league_modes"));
  }

  @Test
  void decodesStructuredModesAndFallsBackForPlainStrings() {
    List<Object> decoded =
        RocketLeagueModesCodec.decodeRocketLeagueModes(
            List.of(
                "{\"mode\":\"2v2\",\"rank\":\"Champion\",\"ratingValue\":1500,\"ratingLabel\":\"MMR\"}",
                "  ",
                "3v3"),
            objectMapper);

    assertEquals(2, decoded.size());
    Map<?, ?> structured = assertInstanceOf(Map.class, decoded.get(0));
    assertEquals("2v2", structured.get("mode"));
    assertEquals("Champion", structured.get("rank"));

    Map<?, ?> fallback = assertInstanceOf(Map.class, decoded.get(1));
    assertEquals("3v3", fallback.get("mode"));
    assertEquals("", fallback.get("rank"));
    assertEquals("MMR", fallback.get("ratingLabel"));
  }

  @Test
  void encodesListsAndMapsAndRejectsInvalidTopLevelPayload() {
    List<String> encoded =
        RocketLeagueModesCodec.encodeRocketLeagueModes(
            new ArrayList<>(
                java.util.Arrays.asList(
                    Map.of("mode", "2v2", "rank", "Grand Champion"),
                    List.of("duo", "queue"),
                    "solo",
                    "   ",
                    null)),
            objectMapper);

    assertEquals(3, encoded.size());
    assertEquals("solo", encoded.get(2));

    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () -> RocketLeagueModesCodec.encodeRocketLeagueModes("bad", objectMapper));
    assertEquals("Expected rocketLeagueModes to be an array.", failure.getReason());
  }
}
