package pt.upt.studenthub;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import java.util.List;

public class TimetableActivity extends AppCompatActivity {
    public static final String EXTRA_STUDENT_NAME = "pt.upt.studenthub.STUDENT_NAME";
    private static final String KEY_TAPS = "key_taps";

    private TimetableViewModel viewModel;
    private TextView tvStatus;
    private ProgressBar progressBar;
    private Button btnSync;
    private Button btnCancel;
    private Button btnTap;
    private LinearLayout sessionsContainer;
    private int taps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable); // layout or layout-land

        // Week 5: Up navigation (parent declared in the manifest)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Week 4: read the Intent extra
        String name = getIntent().getStringExtra(EXTRA_STUDENT_NAME);
        if (name == null) {
            name = "";
        }

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(getString(R.string.timetable_title, name));

        tvStatus = findViewById(R.id.tvStatus);
        progressBar = findViewById(R.id.progressBar);
        btnSync = findViewById(R.id.btnSync);
        btnCancel = findViewById(R.id.btnCancel);
        btnTap = findViewById(R.id.btnTap);
        sessionsContainer = findViewById(R.id.sessionsContainer);

        // Week 5: small UI state restored from the Bundle
        if (savedInstanceState != null) {
            taps = savedInstanceState.getInt(KEY_TAPS, 0);
        }
        updateTapButton();

        // Week 6: same ViewModel instance after rotation
        viewModel = new ViewModelProvider(this).get(TimetableViewModel.class);

        btnSync.setOnClickListener(v ->
                viewModel.startSync(getResources().getStringArray(R.array.sessions))
        );
        btnCancel.setOnClickListener(v -> viewModel.cancelSync());
        btnTap.setOnClickListener(v -> {
            taps++;
            updateTapButton();
        });

        // Observers: called on the UI thread, only while the Activity is visible
        viewModel.getProgress().observe(this, p -> {
            progressBar.setProgress(p);
            if (viewModel.getState().getValue() == TimetableViewModel.State.RUNNING) {
                tvStatus.setText(getString(R.string.status_running, p));
            }
        });

        viewModel.getSessions().observe(this, this::renderSessions);
        viewModel.getState().observe(this, this::renderState);
    }

    private void updateTapButton() {
        btnTap.setText(getString(R.string.btn_tap, taps));
    }

    private void renderState(TimetableViewModel.State s) {
        boolean running = s == TimetableViewModel.State.RUNNING;
        btnSync.setEnabled(!running);
        btnCancel.setEnabled(running);

        switch (s) {
            case IDLE:
                tvStatus.setText(R.string.status_idle);
                break;
            case RUNNING:
                Integer p = viewModel.getProgress().getValue();
                tvStatus.setText(getString(R.string.status_running, p == null ? 0 : p));
                break;
            case DONE:
                List<String> items = viewModel.getSessions().getValue();
                tvStatus.setText(getString(R.string.status_done, items == null ? 0 : items.size()));
                break;
            case CANCELLED:
                tvStatus.setText(R.string.status_cancelled);
                break;
        }
        invalidateOptionsMenu(); // refresh enabled state of "Clear"
    }

    /** Adds only the new sessions; each one enters with an XML animation. */
    private void renderSessions(List<String> items) {
        if (items.size() < sessionsContainer.getChildCount()) {
            sessionsContainer.removeAllViews(); // list was cleared/restarted
        }
        int pad = getResources().getDimensionPixelSize(R.dimen.spacing_s);
        for (int i = sessionsContainer.getChildCount(); i < items.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(items.get(i));
            tv.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
            tv.setMinHeight(getResources().getDimensionPixelSize(R.dimen.touch_target));
            tv.setPadding(0, pad, 0, pad);
            sessionsContainer.addView(tv);
            tv.startAnimation(AnimationUtils.loadAnimation(this, R.anim.item_enter));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAPS, taps);
    }

    // --- Options Menu (Week 5) ---

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_timetable, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem clear = menu.findItem(R.id.action_clear);
        clear.setEnabled(viewModel.getState().getValue() != TimetableViewModel.State.RUNNING);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear) {
            viewModel.clear();
            return true;
        } else if (id == R.id.action_block_ui) {
            blockUiThreadOnPurpose();
            return true;
        } else if (id == R.id.action_about) {
            AboutDialog.show(this);
            return true;
        }
        // android.R.id.home (Up) is handled by AppCompatActivity
        return super.onOptionsItemSelected(item);
    }

    /** X BAD PRACTICE - demonstration only (jank / ANR / StrictMode). */
    private void blockUiThreadOnPurpose() {
        StrictMode.noteSlowCall("Blocking the UI thread on purpose");
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Toast.makeText(this, R.string.msg_blocked, Toast.LENGTH_SHORT).show();
    }
}