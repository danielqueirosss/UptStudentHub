package pt.upt.studenthub;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.Locale;

/** Ecrã da lista marítima: observa o ViewModel e converte estado/modelos em interface. */
public class ExplorerActivity extends AppCompatActivity {
    private static final String KEY_LIST_POSITION = "explorer.list.position";
    private ExplorerViewModel viewModel;
    private ItemAdapter adapter;
    private ListView list;
    private TextView beach, status, retained, count, threshold;
    private ProgressBar progress;
    private Button refresh, cancel;
    private Parcelable pendingListPosition;
    private boolean restoring;
    private Beach displayedBeach;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        String name = getIntent().getStringExtra(TimetableActivity.EXTRA_STUDENT_NAME);
        if (name == null || name.trim().isEmpty()) {
            name = getString(R.string.explorer_no_name);
        }
        TextView greeting = findViewById(R.id.tvExplorerGreeting);
        greeting.setText(getString(R.string.explorer_greeting, name));
        beach = findViewById(R.id.tvExplorerBeach);
        status = findViewById(R.id.tvExplorerStatus);
        retained = findViewById(R.id.tvExplorerRetained);
        count = findViewById(R.id.tvExplorerCount);
        threshold = findViewById(R.id.tvExplorerThreshold);
        progress = findViewById(R.id.progressExplorer);
        refresh = findViewById(R.id.btnExplorerRefresh);
        cancel = findViewById(R.id.btnExplorerCancel);
        list = findViewById(R.id.listExplorer);
        adapter = new ItemAdapter(this);
        list.setAdapter(adapter);
        if (savedInstanceState != null) {
            pendingListPosition = savedInstanceState.getParcelable(KEY_LIST_POSITION);
            restoring = true;
        }
        viewModel = new ViewModelProvider(this).get(ExplorerViewModel.class);
        refresh.setOnClickListener(v -> viewModel.refresh());
        cancel.setOnClickListener(v -> viewModel.cancel());
        viewModel.getState().observe(this, this::render);
        viewModel.getFavorites().observe(this, adapter::submitFavorites);
        viewModel.loadIfNeeded();
    }

    private void render(ExplorerViewModel.UiState state) {
        beach.setText(state.beach.nameRes);
        boolean loading = state.status == ExplorerViewModel.Status.LOADING;
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        refresh.setEnabled(!loading);
        cancel.setEnabled(loading);
        status.setText(statusText(state));

        boolean hasData = !state.items.isEmpty();
        boolean showRetained = hasData && state.status != ExplorerViewModel.Status.SUCCESS;
        retained.setVisibility(showRetained ? View.VISIBLE : View.GONE);
        if (showRetained && state.dataBeach != null) {
            retained.setText(getString(R.string.explorer_retained_data, getString(state.dataBeach.nameRes)));
        }
        count.setVisibility(hasData ? View.VISIBLE : View.GONE);
        threshold.setVisibility(hasData ? View.VISIBLE : View.GONE);
        count.setText(getResources().getQuantityString(
                R.plurals.explorer_hour_count, state.items.size(), state.items.size()));
        ExplorerSettings displayedSettings = state.dataSettings == null ? state.settings : state.dataSettings;
        threshold.setText(String.format(new Locale("pt", "PT"),
                getString(R.string.explorer_surf_threshold), displayedSettings.waveThresholdMetres));
        boolean changed = adapter.submitItems(state.items, displayedSettings);
        if (pendingListPosition != null && hasData) {
            list.onRestoreInstanceState(pendingListPosition);
            pendingListPosition = null;
        } else if (hasData && displayedBeach != null && displayedBeach != state.dataBeach) {
            list.setSelection(0);
        }
        displayedBeach = state.dataBeach;
        if (changed && state.status == ExplorerViewModel.Status.SUCCESS && !restoring) {
            list.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(this, R.anim.explorer_list_enter));
            list.scheduleLayoutAnimation();
        }
        restoring = false;
        invalidateOptionsMenu();
    }

    private String statusText(ExplorerViewModel.UiState state) {
        switch (state.status) {
            case LOADING:
                return getString(R.string.explorer_loading, getString(state.beach.nameRes));
            case SUCCESS:
                return getString(R.string.explorer_success);
            case EMPTY:
                return getString(R.string.explorer_empty);
            case CANCELLED:
                return getString(R.string.explorer_cancelled);
            case ERROR:
                switch (state.error) {
                    case HTTP: return getString(R.string.err_http, state.httpCode);
                    case TIMEOUT: return getString(R.string.err_timeout);
                    case PARSE: return getString(R.string.err_parse);
                    default: return getString(R.string.err_no_connection);
                }
            default:
                return getString(R.string.explorer_idle);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelable(KEY_LIST_POSITION,
                pendingListPosition == null ? list.onSaveInstanceState() : pendingListPosition);
        super.onSaveInstanceState(outState);
    }

    /** Cria as ações já disponíveis; comparação e definições serão ligadas nas suas etapas. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_explorer, menu);
        return true;
    }

    /** Impede novos pedidos durante LOADING e assinala a praia selecionada. */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ExplorerViewModel.UiState state = viewModel.getState().getValue();
        if (state != null) {
            boolean enabled = state.status != ExplorerViewModel.Status.LOADING;
            menu.findItem(R.id.action_refresh).setEnabled(enabled);
            MenuItem beaches = menu.findItem(R.id.action_choose_beach);
            beaches.setEnabled(enabled);
            if (beaches.getSubMenu() != null) {
                beaches.getSubMenu().setGroupEnabled(R.id.group_beaches, enabled);
            }
            int selected = state.beach == Beach.MATOSINHOS ? R.id.action_beach_matosinhos
                    : state.beach == Beach.POVOA_DE_VARZIM ? R.id.action_beach_povoa
                    : R.id.action_beach_nazare;
            // Num grupo exclusivo, assinalar apenas o selecionado desmarca automaticamente os outros.
            menu.findItem(selected).setChecked(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    /** Executa ações de menu através do ViewModel, sem fazer rede na Activity. */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            viewModel.refresh();
        } else if (id == R.id.action_beach_matosinhos) {
            viewModel.selectBeach(Beach.MATOSINHOS);
        } else if (id == R.id.action_beach_povoa) {
            viewModel.selectBeach(Beach.POVOA_DE_VARZIM);
        } else if (id == R.id.action_beach_nazare) {
            viewModel.selectBeach(Beach.NAZARE);
        } else if (id == R.id.action_about) {
            AboutDialog.show(this);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /** Regressa à MainActivity existente, preservando o nome introduzido. */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
