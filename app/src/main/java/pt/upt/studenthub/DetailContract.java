package pt.upt.studenthub;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

/** Extras simples para abrir o detalhe e devolver o favorito, como na Semana 4. */
public final class DetailContract {
    public static final String EXTRA_KEY = "detail.item_key";
    public static final String EXTRA_FAVORITE = "detail.favorite";
    private static final String BEACH = "detail.beach";
    private static final String TIME = "detail.time";
    private static final String HEIGHT = "detail.wave_height";
    private static final String PERIOD = "detail.wave_period";
    private static final String DIRECTION = "detail.wave_direction";
    private static final String WATER = "detail.water_temperature";

    private DetailContract() { }

    /** Transfere apenas os dados da linha; valores nulos são representados por extras ausentes. */
    public static Intent createIntent(Context context, MarineHour item, boolean favorite) {
        Intent intent = new Intent(context, DetailActivity.class);
        intent.putExtra(BEACH, item.beach.id);
        intent.putExtra(TIME, item.time);
        intent.putExtra(EXTRA_FAVORITE, favorite);
        putNumber(intent, HEIGHT, item.waveHeight);
        putNumber(intent, PERIOD, item.wavePeriod);
        putNumber(intent, DIRECTION, item.waveDirection);
        putNumber(intent, WATER, item.waterTemperature);
        return intent;
    }

    /** Reconstrói o modelo; extras inválidos não devem iniciar um pedido nem causar um crash. */
    @Nullable
    public static MarineHour readItem(Intent intent) {
        try {
            Beach beach = Beach.fromId(intent.getStringExtra(BEACH));
            String time = intent.getStringExtra(TIME);
            if (!ExplorerTime.isValid(time)) {
                return null;
            }
            return new MarineHour(beach, time, readNumber(intent, HEIGHT),
                    readNumber(intent, DIRECTION), readNumber(intent, PERIOD), readNumber(intent, WATER));
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    private static void putNumber(Intent intent, String key, @Nullable Double value) {
        if (value != null) {
            intent.putExtra(key, value.doubleValue());
        }
    }

    @Nullable
    private static Double readNumber(Intent intent, String key) {
        if (!intent.hasExtra(key)) {
            return null;
        }
        double value = intent.getDoubleExtra(key, Double.NaN);
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Invalid detail measurement");
        }
        return value;
    }
}
