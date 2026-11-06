package pt.upt.studenthub;
import android.net.Uri;
/**
 * Builds the URLs of the Open-Meteo web service (free, HTTPS, no API key).
 * Uri.Builder encodes every part correctly (commas, slashes, spaces...).
 */
public final class WeatherApi {
    // Universidade Portucalense, Porto
    public static final double UPT_LATITUDE = 41.18;
    public static final double UPT_LONGITUDE = -8.61;
    private WeatherApi() { }
    /** https://api.openmeteo.com/v1/forecast?latitude=...&longitude=...&current=...&timezone=... */
    public static String currentWeatherUrl() {
        return build("forecast");
    }
    /** Same request to a path that does not exist: the server answers with an HTTP error.
     */
    public static String brokenUrl() {
        return build("forecast-does-not-exist");
    }
    private static String build(String path) {
        return new Uri.Builder()
                .scheme("https")
                .authority("api.open-meteo.com")
                .appendPath("v1")
                .appendPath(path)
                .appendQueryParameter("latitude", String.valueOf(UPT_LATITUDE))
                .appendQueryParameter("longitude", String.valueOf(UPT_LONGITUDE))
                .appendQueryParameter("current",
                        "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code")
                .appendQueryParameter("timezone", "Europe/Lisbon")
                .build()
                .toString();
    }
}