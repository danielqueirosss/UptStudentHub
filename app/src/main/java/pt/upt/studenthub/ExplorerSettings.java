package pt.upt.studenthub;

/** Definições imutáveis em memória; a Activity apresenta os erros nos campos. */
public final class ExplorerSettings {
    public static final double MIN_WAVE_THRESHOLD = 0.5;
    public static final double MAX_WAVE_THRESHOLD = 5.0;
    public static final int MIN_REFRESH_MINUTES = 1;
    public static final int MAX_REFRESH_MINUTES = 60;

    public final double waveThresholdMetres;
    public final int hourStep;
    public final int forecastDays;
    public final int refreshMinutes;

    /** Cria definições válidas, rejeitando valores fora dos limites. */
    public ExplorerSettings(double waveThresholdMetres, int hourStep,
                            int forecastDays, int refreshMinutes) {
        if (!isValidWaveThreshold(waveThresholdMetres)) {
            throw new IllegalArgumentException("Invalid wave threshold");
        }
        if (hourStep != 1 && hourStep != 3) {
            throw new IllegalArgumentException("Hour step must be 1 or 3");
        }
        if (forecastDays != 1 && forecastDays != 2) {
            throw new IllegalArgumentException("Forecast days must be 1 or 2");
        }
        if (!isValidRefreshMinutes(refreshMinutes)) {
            throw new IllegalArgumentException("Invalid refresh interval");
        }
        this.waveThresholdMetres = waveThresholdMetres;
        this.hourStep = hourStep;
        this.forecastDays = forecastDays;
        this.refreshMinutes = refreshMinutes;
    }

    /** Valores iniciais: limiar de 1,5 m, todas as horas, dois dias e cinco minutos. */
    public static ExplorerSettings defaults() {
        return new ExplorerSettings(1.5, 1, 2, 5);
    }

    /** Verifica o intervalo exigido pelo enunciado, incluindo NaN e infinito. */
    public static boolean isValidWaveThreshold(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value)
                && value >= MIN_WAVE_THRESHOLD && value <= MAX_WAVE_THRESHOLD;
    }

    /** Limites de desenho da aplicação: de um a sessenta minutos. */
    public static boolean isValidRefreshMinutes(int value) {
        return value >= MIN_REFRESH_MINUTES && value <= MAX_REFRESH_MINUTES;
    }

    /** Converte o intervalo para a unidade de Handler.postDelayed. */
    public long refreshIntervalMillis() {
        return refreshMinutes * 60_000L;
    }
}
