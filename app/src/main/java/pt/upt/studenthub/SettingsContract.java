package pt.upt.studenthub;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;

/** Passagem das quatro definições por extras, sem persistência ou objetos Android no modelo. */
public final class SettingsContract {
    private static final String THRESHOLD = "settings.wave_threshold";
    private static final String STEP = "settings.hour_step";
    private static final String DAYS = "settings.forecast_days";
    private static final String REFRESH = "settings.refresh_minutes";

    private SettingsContract() { }

    /** Abre as definições com os valores atuais do Explorer. */
    public static Intent createIntent(Context context, ExplorerSettings settings) {
        return write(new Intent(context, SettingsActivity.class), settings);
    }

    /** Prepara o resultado apenas depois da validação do formulário. */
    public static Intent result(ExplorerSettings settings) {
        return write(new Intent(), settings);
    }

    /** Revalida os extras recebidos; valores ausentes ou inválidos não são aplicados. */
    @Nullable
    public static ExplorerSettings read(Intent intent) {
        if (intent == null || !intent.hasExtra(THRESHOLD) || !intent.hasExtra(STEP)
                || !intent.hasExtra(DAYS) || !intent.hasExtra(REFRESH)) {
            return null;
        }
        try {
            return new ExplorerSettings(intent.getDoubleExtra(THRESHOLD, Double.NaN),
                    intent.getIntExtra(STEP, -1), intent.getIntExtra(DAYS, -1),
                    intent.getIntExtra(REFRESH, -1));
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    private static Intent write(Intent intent, ExplorerSettings settings) {
        return intent.putExtra(THRESHOLD, settings.waveThresholdMetres)
                .putExtra(STEP, settings.hourStep)
                .putExtra(DAYS, settings.forecastDays)
                .putExtra(REFRESH, settings.refreshMinutes);
    }
}
