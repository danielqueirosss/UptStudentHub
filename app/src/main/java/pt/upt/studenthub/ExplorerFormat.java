package pt.upt.studenthub;

import android.content.Context;
import androidx.annotation.Nullable;
import java.util.Locale;

/** Formatação comum à lista e ao detalhe; esta classe pertence apenas à interface. */
public final class ExplorerFormat {
    private static final Locale PORTUGUESE = new Locale("pt", "PT");
    private ExplorerFormat() { }

    /** Mostra o limiar configurado sem o arredondar para um valor diferente. */
    public static String decimal(double value) {
        return Double.toString(value).replace('.', ',');
    }

    /** Formata uma medição ou a indicação de indisponibilidade, sem substituir null por zero. */
    public static String measurement(Context context, @Nullable Double value, int formatRes) {
        return value == null ? context.getString(R.string.explorer_unavailable)
                : String.format(PORTUGUESE, context.getString(formatRes), value);
    }

    /** Formata direção de origem em graus e ponto cardeal. */
    public static String direction(Context context, @Nullable Double degrees) {
        if (degrees == null) {
            return context.getString(R.string.explorer_unavailable);
        }
        String[] points = context.getResources().getStringArray(R.array.explorer_cardinal_points);
        double normalized = (degrees % 360 + 360) % 360;
        int point = (int) Math.floor((normalized + 22.5) / 45.0) % points.length;
        return String.format(PORTUGUESE, context.getString(R.string.explorer_degrees), normalized, points[point]);
    }

    /** Apresenta a data e a hora local que já foram validadas pelo parser/contrato. */
    public static String time(Context context, String isoTime) {
        return context.getString(R.string.explorer_date_hour,
                isoTime.substring(8, 10), isoTime.substring(5, 7), isoTime.substring(11));
    }
}
