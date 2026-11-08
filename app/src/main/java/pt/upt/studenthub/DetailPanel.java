package pt.upt.studenthub;

import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/** Controla apenas as Views do painel de paisagem; os dados e pedidos vivem nos ViewModels. */
final class DetailPanel {
    private final AppCompatActivity activity;
    private final ExplorerViewModel explorer;
    private final DetailViewModel detail;
    private final View content, placeholder;
    private AlertDialog removalDialog;
    private String confirmationKey;
    private WeatherHour displayedReport;
    private boolean restoring;
    private String displayedKey;

    DetailPanel(AppCompatActivity activity, ExplorerViewModel explorer, DetailViewModel detail,
                boolean restoring) {
        this.activity = activity;
        this.explorer = explorer;
        this.detail = detail;
        this.restoring = restoring;
        content = activity.findViewById(R.id.explorerDetailContent);
        // Os insets já são tratados pelo contentor da ExplorerActivity.
        content.setFitsSystemWindows(false);
        placeholder = activity.findViewById(R.id.tvSelectDetail);
        content.findViewById(R.id.btnDetailRefresh).setOnClickListener(v -> detail.refresh());
        content.findViewById(R.id.btnDetailFavorite).setOnClickListener(v -> {
            MarineHour item = detail.getSelected();
            if (item == null) return;
            if (explorer.isFavorite(item.key())) confirmRemoval();
            else changeFavorite(item.key(), true);
        });
        detail.getState().observe(activity, state -> render());
        explorer.getFavorites().observe(activity, keys -> render());
    }

    void render() {
        MarineHour item = detail.getSelected();
        if (confirmationKey != null && (item == null || !confirmationKey.equals(item.key()))) closeDialog();
        content.setVisibility(item == null ? View.GONE : View.VISIBLE);
        placeholder.setVisibility(item == null ? View.VISIBLE : View.GONE);
        if (item == null) { displayedKey = null; return; }
        if (!item.key().equals(displayedKey)) {
            content.scrollTo(0, 0);
            displayedKey = item.key();
        }
        text(R.id.tvDetailBeach, activity.getString(item.beach.nameRes));
        text(R.id.tvDetailTime, ExplorerFormat.time(activity, item.time));
        measurement(R.id.tvDetailHeight, R.string.explorer_height, item.waveHeight, R.string.explorer_metres);
        measurement(R.id.tvDetailPeriod, R.string.explorer_period, item.wavePeriod, R.string.explorer_seconds);
        text(R.id.tvDetailDirection, activity.getString(R.string.explorer_direction,
                ExplorerFormat.direction(activity, item.waveDirection)));
        measurement(R.id.tvDetailWater, R.string.explorer_water, item.waterTemperature, R.string.explorer_celsius);
        ((Button) content.findViewById(R.id.btnDetailFavorite)).setText(explorer.isFavorite(item.key())
                ? R.string.detail_unmark_favorite : R.string.detail_mark_favorite);
        DetailViewModel.UiState state = detail.getState().getValue();
        if (state == null) return;
        boolean loading = state.status == ExplorerViewModel.Status.LOADING;
        ExplorerViewModel.UiState listState = explorer.getState().getValue();
        content.findViewById(R.id.btnDetailRefresh).setEnabled(!loading && listState != null
                && listState.status != ExplorerViewModel.Status.LOADING);
        content.findViewById(R.id.progressDetail).setVisibility(loading ? View.VISIBLE : View.GONE);
        text(R.id.tvDetailStatus, statusText(state));
        WeatherHour report = state.report;
        content.findViewById(R.id.detailWeatherValues).setVisibility(report == null ? View.GONE : View.VISIBLE);
        if (report != null) {
            measurement(R.id.tvDetailWindSpeed, R.string.detail_wind_speed, report.windSpeed, R.string.detail_kmh);
            text(R.id.tvDetailWindDirection, activity.getString(R.string.detail_wind_direction,
                    ExplorerFormat.direction(activity, report.windDirection)));
            measurement(R.id.tvDetailAir, R.string.detail_air_temperature, report.airTemperature, R.string.explorer_celsius);
            if (state.status == ExplorerViewModel.Status.SUCCESS && displayedReport != report && !restoring) {
                content.findViewById(R.id.detailCard).startAnimation(AnimationUtils.loadAnimation(activity, R.anim.item_enter));
            }
        }
        displayedReport = report;
        restoring = false;
    }

    private void text(int id, String value) { ((TextView) content.findViewById(id)).setText(value); }

    private void measurement(int id, int label, Double value, int unit) {
        text(id, activity.getString(label, ExplorerFormat.measurement(activity, value, unit)));
    }

    private String statusText(DetailViewModel.UiState state) {
        switch (state.status) {
            case LOADING: return activity.getString(R.string.detail_loading);
            case SUCCESS: return activity.getString(R.string.detail_ready);
            case EMPTY: return activity.getString(R.string.detail_empty);
            case CANCELLED: return activity.getString(R.string.explorer_cancelled);
            case ERROR:
                switch (state.error) {
                    case HTTP: return activity.getString(R.string.err_http, state.httpCode);
                    case TIMEOUT: return activity.getString(R.string.err_timeout);
                    case PARSE: return activity.getString(R.string.err_parse);
                    default: return activity.getString(R.string.err_no_connection);
                }
            default: return activity.getString(R.string.detail_idle);
        }
    }

    void confirmRemoval() {
        MarineHour item = detail.getSelected();
        if (item == null || !explorer.isFavorite(item.key()) || confirmationKey != null) return;
        final String key = item.key();
        confirmationKey = key;
        removalDialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.detail_remove_title)
                .setMessage(activity.getString(R.string.detail_remove_message,
                        activity.getString(item.beach.nameRes), ExplorerFormat.time(activity, item.time)))
                .setNegativeButton(R.string.btn_cancel, null)
                .setPositiveButton(R.string.detail_remove_confirm, (dialog, which) -> changeFavorite(key, false))
                .create();
        removalDialog.setOnDismissListener(dialog -> confirmationKey = null);
        removalDialog.show();
    }

    boolean isConfirming() { return confirmationKey != null; }

    private void changeFavorite(String key, boolean marked) {
        explorer.setFavorite(key, marked);
        Toast.makeText(activity, marked ? R.string.detail_favorite_added
                : R.string.detail_favorite_removed, Toast.LENGTH_SHORT).show();
    }

    void closeDialog() {
        if (removalDialog != null) removalDialog.dismiss();
        confirmationKey = null;
    }
}
