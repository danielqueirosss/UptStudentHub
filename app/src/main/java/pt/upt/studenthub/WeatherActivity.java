package pt.upt.studenthub;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.NetworkOnMainThreadException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import java.io.IOException;
/**
 * Module 2 — Campus weather (Week 7: networking + broadcast receivers).
 * The Activity only does UI work; the HTTP request runs in WeatherViewModel.
 */
public class WeatherActivity extends AppCompatActivity {
    private WeatherViewModel viewModel;
    private ConnectivityManager connectivityManager;
    private TextView tvOffline;
    private TextView tvStatus;
    private TextView tvTemperature;
    private TextView tvDescription;
    private TextView tvHumidity;
    private TextView tvWind;
    private View weatherCard;
    private ProgressBar progressLoading;
    private Button btnRefresh;
    private CheckBox cbAutoRefresh;
    private boolean online = true;
    /** BroadcastReceiver: the system broadcasts an Intent whenever airplane mode changes.
     */
    private final BroadcastReceiver airplaneModeReceiver = new BroadcastReceiver() {
        /** Trata o evento do sistema na main thread, sem executar pedidos de rede. */
        @Override
        public void onReceive(Context context, Intent intent) {
            // Runs on the MAIN thread: keep it short (no network, no disk here)
            boolean enabled = intent.getBooleanExtra("state", false);
            Toast.makeText(context,
                    enabled ? R.string.toast_airplane_on : R.string.toast_airplane_off,
                    Toast.LENGTH_SHORT).show();
        }
    };
    /** NetworkCallback: modern replacement of the deprecated CONNECTIVITY_ACTION
     broadcast. */
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                /** Encaminha a disponibilidade da rede para a main thread. */
                @Override
                public void onAvailable(@NonNull Network network) {
                    // Called on a BACKGROUND thread -> go back to the UI thread (Week 6, rule 2)
                    runOnUiThread(() -> setOnline(true));
                }
                /** Encaminha a perda da rede para a main thread. */
                @Override
                public void onLost(@NonNull Network network) {
                    runOnUiThread(() -> setOnline(false));
                }
            };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        String name = getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME);
        if (name == null) {
            name = "";
        }
        TextView tvGreeting = findViewById(R.id.tvGreeting);
        tvGreeting.setText(getString(R.string.weather_title, name));
        tvOffline = findViewById(R.id.tvOffline);
        tvStatus = findViewById(R.id.tvWeatherStatus);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvDescription = findViewById(R.id.tvDescription);
        tvHumidity = findViewById(R.id.tvHumidity);
        tvWind = findViewById(R.id.tvWind);
        weatherCard = findViewById(R.id.weatherCard);
        progressLoading = findViewById(R.id.progressLoading);
        btnRefresh = findViewById(R.id.btnRefresh);
        cbAutoRefresh = findViewById(R.id.cbAutoRefresh);
        connectivityManager = ContextCompat.getSystemService(this,
                ConnectivityManager.class);
        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        btnRefresh.setOnClickListener(v -> viewModel.load(WeatherApi.currentWeatherUrl()));
        viewModel.getReport().observe(this, this::renderReport);
        viewModel.getStatus().observe(this, this::renderStatus);
        // First visit only: start downloading immediately (after a rotation the status is not IDLE)
        if (viewModel.getStatus().getValue() == WeatherViewModel.Status.IDLE) {
            viewModel.load(WeatherApi.currentWeatherUrl());
        }
    }
    // ---------------- Register / unregister with the lifecycle (Week 4) ----------------
    @Override
    protected void onStart() {
        super.onStart();
        // Android 14+: a context-registered receiver must say whether other apps may sendto it
        ContextCompat.registerReceiver(this, airplaneModeReceiver,
                new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        setOnline(isOnlineNow()); // current state
        connectivityManager.registerDefaultNetworkCallback(networkCallback); // futurechanges
    }
    @Override
    protected void onStop() {
        unregisterReceiver(airplaneModeReceiver);
        connectivityManager.unregisterNetworkCallback(networkCallback);
        super.onStop();
    }
    // ---------------- Network state ----------------
    private boolean isOnlineNow() {
        Network active = connectivityManager.getActiveNetwork();
        if (active == null) {
            return false;
        }
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(active);
        return caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
    private void setOnline(boolean nowOnline) {
        boolean cameBack = !online && nowOnline;
        online = nowOnline;
        tvOffline.setVisibility(online ? View.GONE : View.VISIBLE);
        if (cameBack && cbAutoRefresh.isChecked()
                && viewModel.getStatus().getValue() == WeatherViewModel.Status.ERROR) {
            Toast.makeText(this, R.string.toast_back_online, Toast.LENGTH_SHORT).show();
            viewModel.load(WeatherApi.currentWeatherUrl());
        }
    }
    // ---------------- Rendering ----------------
    private void renderStatus(WeatherViewModel.Status s) {
        boolean loading = s == WeatherViewModel.Status.LOADING;
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!loading);
        switch (s) {
            case IDLE:
                tvStatus.setText(R.string.status_weather_idle);
                break;
            case LOADING:
                tvStatus.setText(R.string.status_loading);
                break;
            case SUCCESS:
                WeatherReport r = viewModel.getReport().getValue();
                tvStatus.setText(getString(R.string.status_success, r == null ? "" :
                        r.hour()));
                break;
            case ERROR:
                tvStatus.setText(errorMessage(viewModel.getError().getValue()));
                break;
        }
        invalidateOptionsMenu();
    }
    /** Converts the error enum (no Context in the ViewModel) into a string resource. */
    private String errorMessage(WeatherViewModel.ErrorType type) {
        if (type == null) {
            return getString(R.string.err_no_connection);
        }
        switch (type) {
            case TIMEOUT:
                return getString(R.string.err_timeout);
            case HTTP:
                return getString(R.string.err_http, viewModel.getLastHttpCode());
            case PARSE:
                return getString(R.string.err_parse);
            default:
                return getString(R.string.err_no_connection);
        }
    }
    private void renderReport(WeatherReport r) {
        if (r == null) {
            weatherCard.setVisibility(View.GONE);
            return;
        }
        tvTemperature.setText(getString(R.string.temp_value, r.temperature));
        tvDescription.setText(WeatherCodes.describe(r.weatherCode));
        tvHumidity.setText(getString(R.string.humidity_value, r.humidity));
        tvWind.setText(getString(R.string.wind_value, r.windSpeed));
        weatherCard.setVisibility(View.VISIBLE);
        weatherCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_enter));
// Week 5
    }
    // ---------------- Options Menu (Week 5) ----------------
    /** Cria as ações do menu a partir dos recursos XML. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_weather, menu);
        return true;
    }
    /** Atualiza a disponibilidade das ações de acordo com o carregamento. */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean loading = viewModel.getStatus().getValue() ==
                WeatherViewModel.Status.LOADING;
        menu.findItem(R.id.action_refresh).setEnabled(!loading);
        menu.findItem(R.id.action_server_error).setEnabled(!loading);
        return super.onPrepareOptionsMenu(menu);
    }
    /** Encaminha a ação do menu para a operação correspondente. */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            viewModel.load(WeatherApi.currentWeatherUrl());
            return true;
        } else if (id == R.id.action_main_thread) {
            fetchOnMainThreadOnPurpose();
            return true;
        } else if (id == R.id.action_server_error) {
            viewModel.load(WeatherApi.brokenUrl());
            return true;
        } else if (id == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    /** ✗ BAD PRACTICE — demonstration only: network on the main thread is forbidden. */
    private void fetchOnMainThreadOnPurpose() {
        try {
            HttpClient.get(WeatherApi.currentWeatherUrl());
        } catch (NetworkOnMainThreadException e) {
            // Thrown by StrictMode (penaltyDeathOnNetwork) before any byte is sent
            Toast.makeText(this, R.string.toast_main_thread, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, R.string.err_no_connection, Toast.LENGTH_LONG).show();
        }
    }
}
