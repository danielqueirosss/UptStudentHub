package pt.upt.studenthub;
import androidx.annotation.StringRes;
/**
 * Maps a WMO weather code (number sent by the server) to a string resource.
 * Returns a resource ID, not text: no Context is needed and all text stays in strings.xml.
 */
public final class WeatherCodes {
    private WeatherCodes() { }
    @StringRes
    public static int describe(int code) {
        if (code == 0) return R.string.wmo_clear;
        if (code == 1) return R.string.wmo_mainly_clear;
        if (code == 2) return R.string.wmo_partly_cloudy;
        if (code == 3) return R.string.wmo_overcast;
        if (code == 45 || code == 48) return R.string.wmo_fog;
        if (code >= 51 && code <= 57) return R.string.wmo_drizzle;
        if (code >= 61 && code <= 67) return R.string.wmo_rain;
        if (code >= 71 && code <= 77) return R.string.wmo_snow;
        if (code >= 80 && code <= 82) return R.string.wmo_showers;
        if (code == 85 || code == 86) return R.string.wmo_snow_showers;
        if (code >= 95 && code <= 99) return R.string.wmo_thunderstorm;
        return R.string.wmo_unknown;
    }
}
