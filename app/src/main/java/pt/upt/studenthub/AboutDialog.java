package pt.upt.studenthub;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;

/** Diálogo de identificação comum aos três módulos. */
public final class AboutDialog {
    private AboutDialog() { }

    /** Apresenta a identificação dos módulos, do aluno e da variante. */
    public static void show(Context context) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
