package pt.upt.studenthub;

import androidx.annotation.Nullable;

/** Meteorologia imutável da hora selecionada, com medições opcionais. */
public final class WeatherHour {
    public final Beach beach;
    public final String time;
    @Nullable public final Double windSpeed;
    @Nullable public final Double windDirection;
    @Nullable public final Double airTemperature;

    /** Guarda vento em km/h, direção de origem em graus e temperatura do ar em °C. */
    public WeatherHour(Beach beach, String time, @Nullable Double windSpeed,
                       @Nullable Double windDirection, @Nullable Double airTemperature) {
        this.beach = beach;
        this.time = time;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.airTemperature = airTemperature;
    }
}
