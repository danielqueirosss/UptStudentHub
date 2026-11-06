package pt.upt.studenthub;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
public class MainActivity extends AppCompatActivity {
    private EditText etName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableStrictModeInDebug();
        setContentView(R.layout.activity_main);
        etName = findViewById(R.id.etName);
        Button btnOpenTimetable = findViewById(R.id.btnOpenTimetable);
        Button btnOpenWeather = findViewById(R.id.btnOpenWeather);
        // Module 1 (Week 6): timetable screen
        btnOpenTimetable.setOnClickListener(v -> openWith(TimetableActivity.class));
        // Module 2 (Week 7): campus weather screen (network)
        btnOpenWeather.setOnClickListener(v -> openWith(WeatherActivity.class));
    }
    /** Validates the name (Week 3) and opens the target screen with an extra (Week 4). */
    private void openWith(Class<?> target) {
        String name = readValidName();
        if (name == null) {
            return;
        }
        Intent intent = new Intent(this, target);
        intent.putExtra(TimetableActivity.EXTRA_STUDENT_NAME, name);
        startActivity(intent);
    }
    /** Returns the trimmed name, or null (and shows an error) if it is empty. */
    @Nullable
    private String readValidName() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError(getString(R.string.error_name_required));
            return null;
        }
        return name;
    }
    /** Week 6 + 7: StrictMode reports slow work and BLOCKS network calls on the main
     thread. */
    private void enableStrictModeInDebug() {
        boolean debuggable =
                (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        if (debuggable) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
// Week 7: network on the main thread -> NetworkOnMainThreadException
                    .penaltyDeathOnNetwork()
                    .build());
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
