package pt.upt.studenthub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/** Mostra o pedido B e devolve o estado do favorito por setResult, sem persistência. */
public class DetailActivity extends AppCompatActivity {
    private static final String TAG = "DetailLifecycle";
    private static final String KEY_FAVORITE = "detail.favorite.state";
    private static final String KEY_CONFIRMATION = "detail.favorite.confirmation";
    private MarineHour item;
    private boolean favorite;
    private boolean confirmationVisible;
    private boolean restoring;
    private AlertDialog removalDialog;
    private View card, weatherValues;
    private TextView status, windSpeed, windDirection, air;
    private ProgressBar progress;
    private Button favoriteButton, refresh;
    private WeatherHour displayedReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        item = DetailContract.readItem(getIntent());
        if (item == null) {
            Toast.makeText(this, R.string.detail_invalid, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_detail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        favorite = savedInstanceState == null
                ? getIntent().getBooleanExtra(DetailContract.EXTRA_FAVORITE, false)
                : savedInstanceState.getBoolean(KEY_FAVORITE);
        restoring = savedInstanceState != null;
        ((TextView) findViewById(R.id.tvDetailBeach)).setText(item.beach.nameRes);
        ((TextView) findViewById(R.id.tvDetailTime)).setText(ExplorerFormat.time(this, item.time));
        setMeasurement(R.id.tvDetailHeight, R.string.explorer_height, item.waveHeight, R.string.explorer_metres);
        setMeasurement(R.id.tvDetailPeriod, R.string.explorer_period, item.wavePeriod, R.string.explorer_seconds);
        ((TextView) findViewById(R.id.tvDetailDirection)).setText(getString(
                R.string.explorer_direction, ExplorerFormat.direction(this, item.waveDirection)));
        setMeasurement(R.id.tvDetailWater, R.string.explorer_water, item.waterTemperature, R.string.explorer_celsius);
        card = findViewById(R.id.detailCard);
        weatherValues = findViewById(R.id.detailWeatherValues);
        status = findViewById(R.id.tvDetailStatus);
        windSpeed = findViewById(R.id.tvDetailWindSpeed);
        windDirection = findViewById(R.id.tvDetailWindDirection);
        air = findViewById(R.id.tvDetailAir);
        progress = findViewById(R.id.progressDetail);
        favoriteButton = findViewById(R.id.btnDetailFavorite);
        refresh = findViewById(R.id.btnDetailRefresh);
        favoriteButton.setOnClickListener(v -> {
            if (favorite) {
                confirmRemoval();
            } else {
                changeFavorite(true);
            }
        });
        publishFavoriteResult();
        DetailViewModel viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        refresh.setOnClickListener(v -> viewModel.refresh());
        viewModel.getState().observe(this, this::render);
        viewModel.loadIfNeeded(item);
        if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_CONFIRMATION) && favorite) {
            confirmRemoval();
        }
    }

    private void setMeasurement(int viewId, int labelId, Double value, int formatId) {
        ((TextView) findViewById(viewId)).setText(getString(labelId,
                ExplorerFormat.measurement(this, value, formatId)));
    }

    private void render(DetailViewModel.UiState state) {
        boolean loading = state.status == ExplorerViewModel.Status.LOADING;
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        refresh.setEnabled(!loading);
        status.setText(statusText(state));
        WeatherHour report = state.report;
        weatherValues.setVisibility(report == null ? View.GONE : View.VISIBLE);
        if (report != null) {
            windSpeed.setText(getString(R.string.detail_wind_speed,
                    ExplorerFormat.measurement(this, report.windSpeed, R.string.detail_kmh)));
            windDirection.setText(getString(R.string.detail_wind_direction,
                    ExplorerFormat.direction(this, report.windDirection)));
            air.setText(getString(R.string.detail_air_temperature,
                    ExplorerFormat.measurement(this, report.airTemperature, R.string.explorer_celsius)));
            if (state.status == ExplorerViewModel.Status.SUCCESS && displayedReport != report && !restoring) {
                card.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_enter));
            }
        }
        displayedReport = report;
        restoring = false;
    }

    private String statusText(DetailViewModel.UiState state) {
        switch (state.status) {
            case LOADING: return getString(R.string.detail_loading);
            case SUCCESS: return getString(R.string.detail_ready);
            case EMPTY: return getString(R.string.detail_empty);
            case CANCELLED: return getString(R.string.explorer_cancelled);
            case ERROR:
                switch (state.error) {
                    case TIMEOUT: return getString(R.string.err_timeout);
                    case HTTP: return getString(R.string.err_http, state.httpCode);
                    case PARSE: return getString(R.string.err_parse);
                    default: return getString(R.string.err_no_connection);
                }
            default: return getString(R.string.detail_idle);
        }
    }

    private void confirmRemoval() {
        if (removalDialog != null && removalDialog.isShowing()) {
            return;
        }
        confirmationVisible = true;
        removalDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.detail_remove_title)
                .setMessage(getString(R.string.detail_remove_message,
                        getString(item.beach.nameRes), ExplorerFormat.time(this, item.time)))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.detail_remove_confirm, (dialog, which) -> changeFavorite(false))
                .create();
        removalDialog.setOnDismissListener(dialog -> confirmationVisible = false);
        removalDialog.show();
    }

    private void changeFavorite(boolean marked) {
        favorite = marked;
        publishFavoriteResult();
        Toast.makeText(this, marked ? R.string.detail_favorite_added
                : R.string.detail_favorite_removed, Toast.LENGTH_SHORT).show();
    }

    private void publishFavoriteResult() {
        favoriteButton.setText(favorite ? R.string.detail_unmark_favorite : R.string.detail_mark_favorite);
        Intent result = new Intent();
        result.putExtra(DetailContract.EXTRA_KEY, item.key());
        result.putExtra(DetailContract.EXTRA_FAVORITE, favorite);
        // O resultado fica preparado para qualquer saída: Up, Back ou gesto do sistema.
        setResult(RESULT_OK, result);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_FAVORITE, favorite);
        outState.putBoolean(KEY_CONFIRMATION, confirmationVisible);
        super.onSaveInstanceState(outState);
    }

    /** Fecha o detalhe com o resultado preparado e regressa à lista existente. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onStart() { super.onStart(); Log.d(TAG, "onStart"); }

    @Override
    protected void onResume() { super.onResume(); Log.d(TAG, "onResume"); }

    @Override
    protected void onPause() { Log.d(TAG, "onPause"); super.onPause(); }

    @Override
    protected void onStop() { Log.d(TAG, "onStop"); super.onStop(); }

    @Override
    protected void onRestart() { super.onRestart(); Log.d(TAG, "onRestart"); }

    @Override
    protected void onDestroy() {
        if (removalDialog != null) {
            removalDialog.dismiss();
        }
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
