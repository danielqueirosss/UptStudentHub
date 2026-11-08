package pt.upt.studenthub;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.json.JSONException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Três pedidos independentes em paralelo; a main thread combina os resultados à medida que chegam. */
public class CompareViewModel extends ViewModel {
    /** Resultado imutável de uma praia, incluindo um eventual erro individual. */
    public static final class BeachResult {
        public final Beach beach;
        public final ExplorerViewModel.Status status;
        public final ExplorerViewModel.ErrorType error;
        public final int httpCode;
        @Nullable public final MarineHour report;

        private BeachResult(Beach beach, ExplorerViewModel.Status status,
                            ExplorerViewModel.ErrorType error, int code, @Nullable MarineHour report) {
            this.beach = beach;
            this.status = status;
            this.error = error;
            this.httpCode = code;
            this.report = report;
        }
    }

    /** Conjunto imutável, sempre na ordem Matosinhos, Póvoa de Varzim e Nazaré. */
    public static final class UiState {
        public final List<BeachResult> results;
        public final boolean simulatedFailure;

        private UiState(List<BeachResult> results, boolean simulatedFailure) {
            this.results = Collections.unmodifiableList(new ArrayList<>(results));
            this.simulatedFailure = simulatedFailure;
        }

        /** Há pelo menos um pedido ainda em execução. */
        public boolean isLoading() {
            for (BeachResult result : results) {
                if (result.status == ExplorerViewModel.Status.LOADING) return true;
            }
            return false;
        }
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object deliveryLock = new Object();
    private final List<Future<?>> tasks = new ArrayList<>();
    private UiState current = initialState();
    private final MutableLiveData<UiState> state = new MutableLiveData<>(current);
    private long generation;
    private boolean started;
    private boolean cleared;

    private static UiState initialState() {
        List<BeachResult> results = new ArrayList<>();
        for (Beach beach : Beach.values()) {
            results.add(new BeachResult(beach, ExplorerViewModel.Status.IDLE,
                    ExplorerViewModel.ErrorType.NONE, 0, null));
        }
        return new UiState(results, false);
    }

    /** Expõe as três colunas sem permitir alterações externas ao estado. */
    public LiveData<UiState> getState() { return state; }

    /** Carrega na primeira abertura; a rotação reutiliza tarefas e resultados existentes. */
    @MainThread
    public void loadIfNeeded() {
        if (!started) refresh(false);
    }

    /** Atualiza as três praias; failNazare é usado exclusivamente pela ação de debug da Activity. */
    @MainThread
    public void refresh(boolean failNazare) {
        if (cleared || current.isLoading()) return;
        started = true;
        long request = ++generation;
        tasks.clear();
        List<BeachResult> loading = new ArrayList<>();
        for (BeachResult previous : current.results) {
            loading.add(new BeachResult(previous.beach, ExplorerViewModel.Status.LOADING,
                    ExplorerViewModel.ErrorType.NONE, 0, previous.report));
            // submit não bloqueia: os três workers podem entrar em HttpClient.get em simultâneo.
            tasks.add(executor.submit(() -> fetch(request, previous.beach,
                    failNazare && previous.beach == Beach.NAZARE)));
        }
        publish(new UiState(loading, failNazare));
    }

    private void fetch(long request, Beach beach, boolean forceError) {
        Log.d("CompareRequest", "start #" + request + " " + beach.id
                + " " + Thread.currentThread().getName());
        try {
            String url = forceError ? ExplorerApi.brokenMarineUrl(beach) : ExplorerApi.comparisonUrl(beach);
            MarineHour report = ExplorerParser.parseCurrent(HttpClient.get(url), beach);
            boolean empty = report.waveHeight == null && report.waveDirection == null
                    && report.wavePeriod == null && report.waterTemperature == null;
            deliver(request, beach, empty ? ExplorerViewModel.Status.EMPTY : ExplorerViewModel.Status.SUCCESS,
                    ExplorerViewModel.ErrorType.NONE, 0, empty ? null : report);
        } catch (HttpClient.HttpStatusException e) {
            fail(request, beach, ExplorerViewModel.ErrorType.HTTP, e.code);
        } catch (SocketTimeoutException e) {
            fail(request, beach, ExplorerViewModel.ErrorType.TIMEOUT, 0);
        } catch (InterruptedIOException e) {
            deliver(request, beach, ExplorerViewModel.Status.CANCELLED,
                    ExplorerViewModel.ErrorType.NONE, 0, null);
        } catch (IOException e) {
            fail(request, beach, ExplorerViewModel.ErrorType.NO_CONNECTION, 0);
        } catch (JSONException e) {
            fail(request, beach, ExplorerViewModel.ErrorType.PARSE, 0);
        }
    }

    private void fail(long request, Beach beach, ExplorerViewModel.ErrorType error, int code) {
        deliver(request, beach, ExplorerViewModel.Status.ERROR, error, code, null);
    }

    private void deliver(long request, Beach beach, ExplorerViewModel.Status status,
                         ExplorerViewModel.ErrorType error, int code, @Nullable MarineHour report) {
        synchronized (deliveryLock) {
            if (cleared) return;
            mainHandler.post(() -> {
                if (cleared || request != generation) return;
                List<BeachResult> results = new ArrayList<>(current.results);
                MarineHour shown = report;
                if (status == ExplorerViewModel.Status.ERROR || status == ExplorerViewModel.Status.CANCELLED) {
                    shown = results.get(beach.ordinal()).report;
                }
                results.set(beach.ordinal(), new BeachResult(beach, status, error, code, shown));
                UiState updated = new UiState(results, current.simulatedFailure);
                if (!updated.isLoading()) tasks.clear();
                Log.d("CompareRequest", "finish #" + request + " " + beach.id + " " + status);
                publish(updated);
            });
        }
    }

    /** Interrompe apenas os pedidos pendentes, conservando os dados já disponíveis. */
    @MainThread
    public void cancel() {
        if (cleared || !current.isLoading()) return;
        invalidateTasks();
        List<BeachResult> results = new ArrayList<>();
        for (BeachResult result : current.results) {
            results.add(result.status == ExplorerViewModel.Status.LOADING
                    ? new BeachResult(result.beach, ExplorerViewModel.Status.CANCELLED,
                    ExplorerViewModel.ErrorType.NONE, 0, result.report) : result);
        }
        publish(new UiState(results, current.simulatedFailure));
    }

    private void publish(UiState value) {
        current = value;
        state.setValue(value);
    }

    private void invalidateTasks() {
        generation++;
        for (Future<?> task : tasks) task.cancel(true);
        tasks.clear();
    }

    /** A saída definitiva cancela os workers e impede publicações após a limpeza. */
    @Override
    protected void onCleared() {
        synchronized (deliveryLock) {
            cleared = true;
            mainHandler.removeCallbacksAndMessages(null);
        }
        invalidateTasks();
        executor.shutdownNow();
        super.onCleared();
    }
}
