package pt.upt.studenthub;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

final class ExplorerTime {
    private ExplorerTime() { }

    /** Valida o calendário sem normalizar datas inválidas, como 31 de fevereiro. */
    static boolean isValid(String time) {
        if (time == null || !time.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}")) {
            return false;
        }
        // UTC é usado só para validar os campos do calendário, não para converter a hora.
        // O texto original continua a representar Europe/Lisbon, incluindo mudanças de DST.
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ROOT);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        ParsePosition position = new ParsePosition(0);
        return format.parse(time, position) != null && position.getIndex() == time.length();
    }
}
