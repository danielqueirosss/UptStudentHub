package pt.upt.studenthub;
import org.json.JSONException;
import org.json.JSONObject;
/**
 * Converts the JSON text sent by the server into a WeatherReport.
 * Week 7 uses the simple org.json API; Week 8 studies JSON in depth (JsonReader).
 *
 * Expected input (shortened):
 * { "current": { "time": "2026-10-15T10:45", "temperature_2m": 17.3,
 * "relative_humidity_2m": 80, "wind_speed_10m": 9.4, "weather_code": 3 } }
 */
public final class WeatherParser {
    private WeatherParser() { }
    /** Converte o JSON da meteorologia do campus num modelo imutável. */
    public static WeatherReport parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json); // whole document
        JSONObject current = root.getJSONObject("current"); // nested object
        return new WeatherReport(
                current.getDouble("temperature_2m"),
                current.getInt("relative_humidity_2m"),
                current.getDouble("wind_speed_10m"),
                current.getInt("weather_code"),
                current.getString("time"));
    }
}
