package pt.upt.studenthub;

import androidx.annotation.StringRes;

/** Praias da variante 3, com as coordenadas originais do enunciado. */
public enum Beach {
    MATOSINHOS("matosinhos", R.string.beach_matosinhos, 41.18, -8.72),
    POVOA_DE_VARZIM("povoa", R.string.beach_povoa, 41.38, -8.78),
    NAZARE("nazare", R.string.beach_nazare, 39.60, -9.08);

    public final String id;
    @StringRes public final int nameRes;
    public final double latitude;
    public final double longitude;

    Beach(String id, @StringRes int nameRes, double latitude, double longitude) {
        this.id = id;
        this.nameRes = nameRes;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /** Recupera uma praia pelo identificador estável usado nos extras. */
    public static Beach fromId(String id) {
        for (Beach beach : values()) {
            if (beach.id.equals(id)) {
                return beach;
            }
        }
        throw new IllegalArgumentException("Unknown beach: " + id);
    }
}
