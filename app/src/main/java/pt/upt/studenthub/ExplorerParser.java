package pt.upt.studenthub;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Converte JSON em modelos imutáveis, preservando nulos e validando arrays paralelos. */
public final class ExplorerParser {
    private ExplorerParser() { }

    /**
     * Lê o pedido A. Arrays vazios produzem uma lista vazia (estado EMPTY no ViewModel).
     */
    public static List<MarineHour> parseMarine(String json, Beach beach,
                                              ExplorerSettings settings)
            throws JSONException, InterruptedIOException {
        checkInterrupted();
        JSONObject hourly = new JSONObject(json).getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        JSONArray heights = alignedArray(hourly, "wave_height", times.length());
        JSONArray directions = alignedArray(hourly, "wave_direction", times.length());
        JSONArray periods = alignedArray(hourly, "wave_period", times.length());
        JSONArray temperatures = alignedArray(hourly, "sea_surface_temperature", times.length());
        List<MarineHour> hours = new ArrayList<>();
        for (int i = 0; i < times.length(); i++) {
            checkInterrupted();
            String time = timestamp(times.get(i));
            MarineHour hour = new MarineHour(beach, time, numberAt(heights, i),
                    numberAt(directions, i), numberAt(periods, i), numberAt(temperatures, i));
            // Seleção pela hora local: 00h, 03h, 06h... e não pela posição de um item na lista.
            int localHour = Integer.parseInt(time.substring(11, 13));
            if (localHour % settings.hourStep == 0) {
                hours.add(hour);
            }
        }
        checkInterrupted();
        return Collections.unmodifiableList(hours);
    }

    /**
     * Lê o pedido B e procura a hora exata, sem assumir índices iguais entre endpoints.
     */
    @Nullable
    public static WeatherHour parseDetail(String json, MarineHour selected)
            throws JSONException, InterruptedIOException {
        checkInterrupted();
        JSONObject hourly = new JSONObject(json).getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        JSONArray speeds = alignedArray(hourly, "wind_speed_10m", times.length());
        JSONArray directions = alignedArray(hourly, "wind_direction_10m", times.length());
        JSONArray temperatures = alignedArray(hourly, "temperature_2m", times.length());
        WeatherHour result = null;
        for (int i = 0; i < times.length(); i++) {
            checkInterrupted();
            String time = timestamp(times.get(i));
            Double speed = numberAt(speeds, i);
            Double direction = numberAt(directions, i);
            Double temperature = numberAt(temperatures, i);
            if (time.equals(selected.time)) {
                result = new WeatherHour(selected.beach, time, speed, direction, temperature);
            }
        }
        checkInterrupted();
        return result;
    }

    /** Lê a secção current de um dos três pedidos da comparação. */
    public static MarineHour parseCurrent(String json, Beach beach)
            throws JSONException, InterruptedIOException {
        checkInterrupted();
        JSONObject current = new JSONObject(json).getJSONObject("current");
        MarineHour result = new MarineHour(beach, timestamp(current.get("time")),
                numberField(current, "wave_height"), numberField(current, "wave_direction"),
                numberField(current, "wave_period"), numberField(current, "sea_surface_temperature"));
        checkInterrupted();
        return result;
    }

    private static JSONArray alignedArray(JSONObject object, String name, int length)
            throws JSONException {
        JSONArray array = object.getJSONArray(name);
        if (array.length() != length) {
            throw new JSONException("Array length differs from time: " + name);
        }
        return array;
    }

    @Nullable
    private static Double numberAt(JSONArray array, int index) throws JSONException {
        // JSON null não é zero: a Activity deverá apresentar uma indicação de indisponibilidade.
        return array.isNull(index) ? null : number(array.get(index));
    }

    @Nullable
    private static Double numberField(JSONObject object, String name) throws JSONException {
        if (!object.has(name)) {
            throw new JSONException("Missing field: " + name);
        }
        return object.isNull(name) ? null : number(object.get(name));
    }

    private static double number(Object value) throws JSONException {
        if (!(value instanceof Number)) {
            throw new JSONException("Expected a number or null");
        }
        double result = ((Number) value).doubleValue();
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new JSONException("Non-finite number");
        }
        return result;
    }

    private static String timestamp(Object value) throws JSONException {
        if (!(value instanceof String) || !ExplorerTime.isValid((String) value)) {
            throw new JSONException("Invalid ISO timestamp");
        }
        return (String) value;
    }

    private static void checkInterrupted() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedIOException("Parsing cancelled");
        }
    }
}
