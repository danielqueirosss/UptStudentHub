package pt.upt.studenthub;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Survives configuration changes (rotation). Owns the background work.
 * RULE: no references to Activity, View or Context here.
 */
public class TimetableViewModel extends ViewModel {
    public enum State { IDLE, RUNNING, DONE, CANCELLED }

    private static final long SIMULATED_DELAY_MS = 800;

    // Worker thread pool (one thread, tasks in order)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // Bridge back to the UI thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Private mutable state
    private final MutableLiveData<State> state = new MutableLiveData<>(State.IDLE);
    private final MutableLiveData<Integer> progress = new MutableLiveData<>(0);
    private final MutableLiveData<List<String>> sessions = new MutableLiveData<>(Collections.emptyList());

    private Future<?> currentTask;

    // ... exposed read-only
    public LiveData<State> getState() { return state; }
    public LiveData<Integer> getProgress() { return progress; }
    public LiveData<List<String>> getSessions() { return sessions; }

    /** Called on the UI thread. Starts the simulated download. */
    public void startSync(String[] source) {
        if (state.getValue() == State.RUNNING) {
            return; // already running: never start twice
        }

        sessions.setValue(Collections.emptyList());
        progress.setValue(0);
        state.setValue(State.RUNNING);

        currentTask = executor.submit(() -> {
            // --- WORKER THREAD from here ---
            List<String> loaded = new ArrayList<>(); // confined to this thread
            try {
                for (int i = 0; i < source.length; i++) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    Thread.sleep(SIMULATED_DELAY_MS); // simulated slow I/O
                    loaded.add(source[i]);

                    // Immutable snapshot: safe to hand to another thread
                    final List<String> snapshot = Collections.unmodifiableList(new ArrayList<>(loaded));
                    final int pct = (i + 1) * 100 / source.length;

                    // --- UI thread ---
                    mainHandler.post(() -> {
                        sessions.setValue(snapshot);
                        progress.setValue(pct);
                    });
                }
                mainHandler.post(() -> state.setValue(State.DONE));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore the flag
                mainHandler.post(() -> state.setValue(State.CANCELLED));
            }
        });
    }

    /** Cooperative cancellation: interrupts the worker thread. */
    public void cancelSync() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    /** Resets everything. Ignored while a sync is running. */
    public void clear() {
        if (state.getValue() == State.RUNNING) {
            return;
        }
        sessions.setValue(Collections.emptyList());
        progress.setValue(0);
        state.setValue(State.IDLE);
    }

    /** Called only when the screen is finished for good (not on rotation). */
    @Override
    protected void onCleared() {
        cancelSync();
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}