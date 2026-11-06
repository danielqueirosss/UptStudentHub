package pt.upt.studenthub;
/**
 * Immutable model with the data we show on screen.
 * "final" fields: once created it never changes, so it is safe to pass between threads.
 */
public final class WeatherReport {
    public final double temperature; // degrees Celsius
    public final int humidity; // percentage 0–100
    public final double windSpeed; // km/h
    public final int weatherCode; // WMO weather code
    public final String time; // local time of the observation, e.g. "2026-10-15T10:45"
    public WeatherReport(double temperature, int humidity, double windSpeed,
                         int weatherCode, String time) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.time = time;
    }
    /** Returns only the hour part of the ISO time ("2026-10-15T10:45" -> "10:45"). */
    public String hour() {
        int t = time.indexOf('T');
        return t >= 0 ? time.substring(t + 1) : time;
    }
}