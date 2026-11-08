package pt.upt.studenthub;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/** Comparação lado a lado, com mensagens e dados independentes para cada praia. */
public class CompareActivity extends AppCompatActivity {
    private CompareViewModel viewModel;
    private TextView summary;
    private View debugNotice;
    private Button refresh, cancel;
    private Column[] columns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        summary = findViewById(R.id.tvCompareSummary);
        debugNotice = findViewById(R.id.tvCompareDebug);
        refresh = findViewById(R.id.btnCompareRefresh);
        cancel = findViewById(R.id.btnCompareCancel);
        columns = new Column[] {new Column(findViewById(R.id.compareMatosinhos)),
                new Column(findViewById(R.id.comparePovoa)), new Column(findViewById(R.id.compareNazare))};
        viewModel = new ViewModelProvider(this).get(CompareViewModel.class);
        refresh.setOnClickListener(v -> viewModel.refresh(false));
        cancel.setOnClickListener(v -> viewModel.cancel());
        viewModel.getState().observe(this, this::render);
        viewModel.loadIfNeeded();
    }

    private void render(CompareViewModel.UiState state) {
        boolean loading = state.isLoading();
        refresh.setEnabled(!loading);
        cancel.setEnabled(loading);
        debugNotice.setVisibility(state.simulatedFailure ? View.VISIBLE : View.GONE);
        int finished = 0;
        int successes = 0;
        for (int i = 0; i < state.results.size(); i++) {
            CompareViewModel.BeachResult result = state.results.get(i);
            if (result.status != ExplorerViewModel.Status.LOADING && result.status != ExplorerViewModel.Status.IDLE) finished++;
            if (result.status == ExplorerViewModel.Status.SUCCESS) successes++;
            renderColumn(columns[i], result);
        }
        summary.setText(getString(loading ? R.string.compare_loading : R.string.compare_finished,
                loading ? finished : successes));
        invalidateOptionsMenu();
    }

    private void renderColumn(Column column, CompareViewModel.BeachResult result) {
        column.name.setText(result.beach.nameRes);
        column.status.setText(statusText(result));
        column.progress.setVisibility(result.status == ExplorerViewModel.Status.LOADING ? View.VISIBLE : View.GONE);
        MarineHour report = result.report;
        column.values.setVisibility(report == null ? View.GONE : View.VISIBLE);
        column.previous.setVisibility(report != null && result.status != ExplorerViewModel.Status.SUCCESS
                ? View.VISIBLE : View.GONE);
        if (report != null) {
            column.time.setText(ExplorerFormat.time(this, report.time));
            column.height.setText(ExplorerFormat.measurement(this, report.waveHeight, R.string.explorer_metres));
            column.period.setText(getString(R.string.explorer_period,
                    ExplorerFormat.measurement(this, report.wavePeriod, R.string.explorer_seconds)));
            column.direction.setText(getString(R.string.explorer_direction,
                    ExplorerFormat.direction(this, report.waveDirection)));
            column.water.setText(getString(R.string.explorer_water,
                    ExplorerFormat.measurement(this, report.waterTemperature, R.string.explorer_celsius)));
        }
    }

    private String statusText(CompareViewModel.BeachResult result) {
        switch (result.status) {
            case LOADING: return getString(R.string.compare_beach_loading);
            case SUCCESS: return getString(R.string.compare_beach_success);
            case EMPTY: return getString(R.string.compare_empty);
            case CANCELLED: return getString(R.string.explorer_cancelled);
            case ERROR:
                switch (result.error) {
                    case HTTP: return getString(R.string.err_http, result.httpCode);
                    case TIMEOUT: return getString(R.string.err_timeout);
                    case PARSE: return getString(R.string.err_parse);
                    default: return getString(R.string.err_no_connection);
                }
            default: return getString(R.string.compare_idle);
        }
    }

    private boolean isDebug() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    /** A simulação usa um URL inválido real e só está disponível na aplicação de debug. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compare, menu);
        menu.findItem(R.id.action_compare_failure).setVisible(isDebug());
        return true;
    }

    /** Desativa comandos que iniciam pedidos enquanto existe trabalho em curso. */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        CompareViewModel.UiState state = viewModel.getState().getValue();
        boolean enabled = state != null && !state.isLoading();
        menu.findItem(R.id.action_refresh).setEnabled(enabled);
        menu.findItem(R.id.action_compare_failure).setEnabled(enabled);
        return super.onPrepareOptionsMenu(menu);
    }

    /** Atualizar repõe pedidos normais; a simulação afeta apenas Nazaré. */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            viewModel.refresh(false);
            return true;
        } else if (item.getItemId() == R.id.action_compare_failure && isDebug()) {
            viewModel.refresh(true);
            return true;
        } else if (item.getItemId() == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Regressa à lista existente, conservando a praia, as definições e os favoritos. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** Referências locais a uma das três instâncias do mesmo layout de coluna. */
    private static final class Column {
        final TextView name, status, time, height, period, direction, water;
        final ProgressBar progress;
        final View values, previous;

        Column(View root) {
            name = root.findViewById(R.id.tvCompareBeach);
            status = root.findViewById(R.id.tvCompareStatus);
            time = root.findViewById(R.id.tvCompareTime);
            height = root.findViewById(R.id.tvCompareHeight);
            period = root.findViewById(R.id.tvComparePeriod);
            direction = root.findViewById(R.id.tvCompareDirection);
            water = root.findViewById(R.id.tvCompareWater);
            progress = root.findViewById(R.id.progressCompareBeach);
            values = root.findViewById(R.id.compareValues);
            previous = root.findViewById(R.id.tvComparePrevious);
        }
    }
}
