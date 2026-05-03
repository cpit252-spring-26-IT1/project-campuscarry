package certifiedcarry_api.shared;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;

public final class PreparedStatementBinder {

  private PreparedStatementBinder() {
  }

  public static void setNullableString(PreparedStatement statement, int index, String value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.VARCHAR);
      return;
    }

    statement.setString(index, value);
  }

  public static void setNullableBigDecimal(PreparedStatement statement, int index, BigDecimal value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.NUMERIC);
      return;
    }

    statement.setBigDecimal(index, value);
  }

  public static void setNullableOffsetDateTime(
      PreparedStatement statement, int index, OffsetDateTime value) throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
      return;
    }

    statement.setObject(index, value);
  }

  public static void setNullableLong(PreparedStatement statement, int index, Long value)
      throws SQLException {
    if (value == null) {
      statement.setNull(index, Types.BIGINT);
      return;
    }

    statement.setLong(index, value);
  }
}
