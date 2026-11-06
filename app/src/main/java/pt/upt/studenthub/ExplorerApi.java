package pt.upt.studenthub;

import android.net.Uri;


public final class ExplorerApi {
    public static final String TIMEZONE = "Europe/Lisbon";
    private static final String MARINE_HOST = "marine-api.open-meteo.com";
    private static final String WEATHER_HOST = "api.open-meteo.com";
    private static final String MARINE_FIELDS =
            "wave_height,wave_direction,wave_period,sea_surface_temperature";

    private ExplorerApi() { }

    /** Pedido A: previsão horária; o intervalo 1 h/3 h é aplicado à lista pelo parser. */
    public static String marineUrl(Beach beach, ExplorerSettings settings) {
        return base(MARINE_HOST, "marine", beach.latitude, beach.longitude)
                .appendQueryParameter("hourly", MARINE_FIELDS)
                .appendQueryParameter("forecast_days", String.valueOf(settings.forecastDays))
                .build().toString();
    }

    /** Pedido B: meteorologia do dia escolhido; o parser seleciona a hora exata. */
    public static String detailUrl(MarineHour item) {
        if (!ExplorerTime.isValid(item.time)) {
            throw new IllegalArgumentException("Invalid observation time");
        }
        String date = item.time.substring(0, 10);
        return base(WEATHER_HOST, "forecast", item.beach.latitude, item.beach.longitude)
                .appendQueryParameter("hourly",
                        "wind_speed_10m,wind_direction_10m,temperature_2m")
                .appendQueryParameter("start_date", date)
                .appendQueryParameter("end_date", date)
                .appendQueryParameter("wind_speed_unit", "kmh")
                .appendQueryParameter("temperature_unit", "celsius")
                .build().toString();
    }

    /** Um pedido de ondulação atual, a executar em paralelo para cada uma das três praias. */
    public static String comparisonUrl(Beach beach) {
        return base(MARINE_HOST, "marine", beach.latitude, beach.longitude)
                .appendQueryParameter("current", MARINE_FIELDS)
                .build().toString();
    }

    public static String brokenMarineUrl(Beach beach) {
        return base(MARINE_HOST, "marine-does-not-exist", beach.latitude, beach.longitude)
                .appendQueryParameter("current", MARINE_FIELDS)
                .build().toString();
    }

    public static String campusWeatherUrl() {
        return campusUrl("forecast");
    }

    /** Preserva a demonstração de erro HTTP do módulo 2. */
    public static String brokenCampusWeatherUrl() {
        return campusUrl("forecast-does-not-exist");
    }

    private static String campusUrl(String path) {
        return base(WEATHER_HOST, path, WeatherApi.UPT_LATITUDE, WeatherApi.UPT_LONGITUDE)
                .appendQueryParameter("current",
                        "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code")
                .build().toString();
    }

    private static Uri.Builder base(String host, String path, double latitude, double longitude) {
        return new Uri.Builder()
                .scheme("https")
                .authority(host)
                .appendPath("v1")
                .appendPath(path)
                .appendQueryParameter("latitude", String.valueOf(latitude))
                .appendQueryParameter("longitude", String.valueOf(longitude))
                .appendQueryParameter("timezone", TIMEZONE);
    }
}
