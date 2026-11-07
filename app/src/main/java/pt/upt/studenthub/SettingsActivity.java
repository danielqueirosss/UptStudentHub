package pt.upt.studenthub;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/** Formulário validado da variante 3, com EditText, RadioGroup e CheckBox. */
public class SettingsActivity extends AppCompatActivity {
    private static final String KEY_THRESHOLD = "form.threshold";
    private static final String KEY_REFRESH = "form.refresh";
    private static final String KEY_STEP = "form.step";
    private static final String KEY_DAYS = "form.days";
    private static final String KEY_THRESHOLD_ERROR = "form.threshold.error";
    private static final String KEY_REFRESH_ERROR = "form.refresh.error";
    private EditText threshold, refresh;
    private RadioGroup hourStep;
    private CheckBox secondDay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ExplorerSettings current = SettingsContract.read(getIntent());
        if (current == null) {
            Toast.makeText(this, R.string.settings_invalid_result, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_settings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        threshold = findViewById(R.id.etSettingsThreshold);
        refresh = findViewById(R.id.etSettingsRefresh);
        hourStep = findViewById(R.id.rgSettingsHourStep);
        secondDay = findViewById(R.id.cbSettingsSecondDay);
        if (savedInstanceState == null) {
            threshold.setText(ExplorerFormat.decimal(current.waveThresholdMetres));
            refresh.setText(String.valueOf(current.refreshMinutes));
            hourStep.check(current.hourStep == 3 ? R.id.rbSettingsThreeHours : R.id.rbSettingsOneHour);
            secondDay.setChecked(current.forecastDays == 2);
        } else {
            // Conservar o texto em edição, mesmo se ainda não constituir um número válido.
            threshold.setText(savedInstanceState.getString(KEY_THRESHOLD));
            refresh.setText(savedInstanceState.getString(KEY_REFRESH));
            hourStep.check(savedInstanceState.getInt(KEY_STEP, R.id.rbSettingsOneHour));
            secondDay.setChecked(savedInstanceState.getBoolean(KEY_DAYS));
            threshold.setError(savedInstanceState.getCharSequence(KEY_THRESHOLD_ERROR));
            refresh.setError(savedInstanceState.getCharSequence(KEY_REFRESH_ERROR));
        }
        findViewById(R.id.btnSettingsSave).setOnClickListener(v -> save());
        findViewById(R.id.btnSettingsCancel).setOnClickListener(v -> finish());
    }

    private void save() {
        threshold.setError(null);
        refresh.setError(null);
        Double wave = readThreshold();
        Integer minutes = readRefreshMinutes();
        if (wave == null || minutes == null) {
            (wave == null ? threshold : refresh).requestFocus();
            return;
        }
        ExplorerSettings result = new ExplorerSettings(wave,
                hourStep.getCheckedRadioButtonId() == R.id.rbSettingsThreeHours ? 3 : 1,
                secondDay.isChecked() ? 2 : 1, minutes);
        setResult(RESULT_OK, SettingsContract.result(result));
        finish();
    }

    private Double readThreshold() {
        String text = threshold.getText().toString().trim();
        try {
            // Aceitar vírgula e ponto decimal, mas não separadores repetidos ou notação científica.
            if (!text.matches("[0-9]+([.,][0-9]+)?")) {
                throw new NumberFormatException();
            }
            double value = Double.parseDouble(text.replace(',', '.'));
            if (!ExplorerSettings.isValidWaveThreshold(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            threshold.setError(getString(R.string.settings_threshold_error));
            return null;
        }
    }

    private Integer readRefreshMinutes() {
        String text = refresh.getText().toString().trim();
        try {
            if (!text.matches("[0-9]+")) {
                throw new NumberFormatException();
            }
            int value = Integer.parseInt(text);
            if (!ExplorerSettings.isValidRefreshMinutes(value)) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            refresh.setError(getString(R.string.settings_refresh_error));
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (threshold != null) {
            outState.putString(KEY_THRESHOLD, threshold.getText().toString());
            outState.putString(KEY_REFRESH, refresh.getText().toString());
            outState.putInt(KEY_STEP, hourStep.getCheckedRadioButtonId());
            outState.putBoolean(KEY_DAYS, secondDay.isChecked());
            outState.putCharSequence(KEY_THRESHOLD_ERROR, threshold.getError());
            outState.putCharSequence(KEY_REFRESH_ERROR, refresh.getError());
        }
        super.onSaveInstanceState(outState);
    }

    /** Up, como Back e Cancelar, descarta alterações que ainda não foram guardadas. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
