package pt.upt.studenthub;

import androidx.annotation.Nullable;

/** Previsão marítima imutável; medições ausentes são representadas por null. */
public final class MarineHour {
    public final Beach beach;
    public final String time;
    @Nullable public final Double waveHeight;
    @Nullable public final Double waveDirection;
    @Nullable public final Double wavePeriod;
    @Nullable public final Double waterTemperature;

    /** Guarda hora local ISO, altura em m, direção em graus, período em s e água em °C. */
    public MarineHour(Beach beach, String time, @Nullable Double waveHeight,
                      @Nullable Double waveDirection, @Nullable Double wavePeriod,
                      @Nullable Double waterTemperature) {
        this.beach = beach;
        this.time = time;
        this.waveHeight = waveHeight;
        this.waveDirection = waveDirection;
        this.wavePeriod = wavePeriod;
        this.waterTemperature = waterTemperature;
    }

    /** Chave do favorito: a mesma hora em praias diferentes representa itens diferentes. */
    public String key() {
        return beach.id + "|" + time;
    }

    /** Regra académica do ícone de surf: altura estritamente superior ao limiar. */
    public boolean exceedsWaveThreshold(ExplorerSettings settings) {
        return waveHeight != null && waveHeight > settings.waveThresholdMetres;
    }
}
