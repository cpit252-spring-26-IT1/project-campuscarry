package certifiedcarry_api.shared;

import java.util.Locale;
import java.util.Map;

public final class GameNameAlias {

  private static final Map<String, String> GAME_NAME_ALIASES =
      Map.of("league of legends", "LoL");

  private GameNameAlias() {}

  public static String normalize(String value) {
    return GAME_NAME_ALIASES.getOrDefault(value.toLowerCase(Locale.ROOT), value);
  }

  public static String normalizeNullable(String value) {
    if (value == null) {
      return null;
    }

    return normalize(value);
  }
}