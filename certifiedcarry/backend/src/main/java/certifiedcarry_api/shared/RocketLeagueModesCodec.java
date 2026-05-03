package certifiedcarry_api.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RocketLeagueModesCodec {

  private RocketLeagueModesCodec() {
  }

  public static List<String> readTextArray(ResultSet resultSet, String columnName)
      throws SQLException {
    Array sqlArray = resultSet.getArray(columnName);
    if (sqlArray == null) {
      return List.of();
    }

    Object rawArray = sqlArray.getArray();
    if (rawArray instanceof String[] values) {
      List<String> normalized = new ArrayList<>();
      for (String value : values) {
        if (value != null) {
          normalized.add(value);
        }
      }
      return normalized;
    }

    if (rawArray instanceof Object[] values) {
      List<String> normalized = new ArrayList<>();
      for (Object value : values) {
        if (value != null) {
          normalized.add(String.valueOf(value));
        }
      }
      return normalized;
    }

    return List.of();
  }

  public static List<Object> decodeRocketLeagueModes(
      List<String> encodedModes, ObjectMapper objectMapper) {
    List<Object> decodedModes = new ArrayList<>();

    for (String encodedMode : encodedModes) {
      String normalized = String.valueOf(encodedMode == null ? "" : encodedMode).trim();
      if (normalized.isEmpty()) {
        continue;
      }

      try {
        Object parsed = objectMapper.readValue(normalized, Object.class);
        if (parsed instanceof Map<?, ?>) {
          decodedModes.add(parsed);
          continue;
        }
      } catch (JsonProcessingException ignored) {
        // Fallback keeps compatibility for rows containing plain mode strings.
      }

      Map<String, Object> fallback = new LinkedHashMap<>();
      fallback.put("mode", normalized);
      fallback.put("rank", "");
      fallback.put("ratingValue", null);
      fallback.put("ratingLabel", "MMR");
      decodedModes.add(fallback);
    }

    return decodedModes;
  }

  public static List<String> encodeRocketLeagueModes(Object value, ObjectMapper objectMapper) {
    if (value == null) {
      return List.of();
    }

    if (!(value instanceof List<?> values)) {
      throw HttpErrors.badRequest("Expected rocketLeagueModes to be an array.");
    }

    List<String> encoded = new ArrayList<>();
    for (Object candidate : values) {
      if (candidate == null) {
        continue;
      }

      if (candidate instanceof Map<?, ?> || candidate instanceof List<?>) {
        try {
          encoded.add(objectMapper.writeValueAsString(candidate));
        } catch (JsonProcessingException exception) {
          throw HttpErrors.badRequest("Invalid rocketLeagueModes entry.");
        }
        continue;
      }

      String normalized = HttpRequestParsers.optionalString(candidate);
      if (normalized != null && !normalized.isBlank()) {
        encoded.add(normalized);
      }
    }

    return encoded;
  }
}
