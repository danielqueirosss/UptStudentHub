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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Executa o pedido B fora da main thread e conserva o resultado durante a rotação. */
public class DetailViewModel extends ViewModel {
    /** Fotografia imutável do pedido de detalhe, com o último resultado válido quando existe. */
    public static final class UiState {
        public final ExplorerViewModel.Status status;
        public final ExplorerViewModel.ErrorType error;
        public final int httpCode;
        @Nullable public final WeatherHour report;

        private UiState(ExplorerViewModel.Status status, ExplorerViewModel.ErrorType error,
                        int httpCode, @Nullable WeatherHour report) {
            this.status = status;
            this.error = error;
            this.httpCode = httpCode;
            this.report = report;
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object deliveryLock = new Object();
    private UiState current = new UiState(ExplorerViewModel.Status.IDLE,
            ExplorerViewModel.ErrorType.NONE, 0, null);
    private final MutableLiveData<UiState> state = new MutableLiveData<>(current);
    private MarineHour selected;
    private Future<?> task;
    private boolean cleared;
    private long generation;

    /** Expõe o estado para observação pelo ecrã sem permitir alterações externas. */
    public LiveData<UiState> getState() {
        return state;
    }

    /** Inicializa uma única vez por ecrã; uma rotação não volta a fazer o pedido B. */
    @MainThread
    public void loadIfNeeded(MarineHour item) {
        if (!cleared && selected == null) {
            selected = item;
            refresh();
        }
    }

    /** Permite repetir um pedido falhado ou atualizar o detalhe sem pedidos duplicados. */
    @MainThread
    public void refresh() {
        if (cleared || selected == null || current.status == ExplorerViewModel.Status.LOADING) {
            return;
        }
        final long request = ++generation;
        final MarineHour item = selected;
        Log.d("DetailRequest", "start #" + request + " " + item.key());
        task = executor.submit(() -> fetch(request, item));
        publish(new UiState(ExplorerViewModel.Status.LOADING,
                ExplorerViewModel.ErrorType.NONE, 0, current.report));
    }

    private void fetch(long request, MarineHour item) {
        try {
            String json = HttpClient.get(ExplorerApi.detailUrl(item));
            WeatherHour report = ExplorerParser.parseDetail(json, item);
            deliver(request, () -> publish(new UiState(report == null
                    ? ExplorerViewModel.Status.EMPTY : ExplorerViewModel.Status.SUCCESS,
                    ExplorerViewModel.ErrorType.NONE, 0, report)));
        } catch (HttpClient.HttpStatusException e) {
            fail(request, ExplorerViewModel.ErrorType.HTTP, e.code);
        } catch (SocketTimeoutException e) {
            fail(request, ExplorerViewModel.ErrorType.TIMEOUT, 0);
        } catch (InterruptedIOException e) {
            deliver(request, () -> publish(new UiState(ExplorerViewModel.Status.CANCELLED,
                    ExplorerViewModel.ErrorType.NONE, 0, current.report)));
        } catch (IOException e) {
            fail(request, ExplorerViewModel.ErrorType.NO_CONNECTION, 0);
        } catch (JSONException e) {
            fail(request, ExplorerViewModel.ErrorType.PARSE, 0);
        }
    }

    private void fail(long request, ExplorerViewModel.ErrorType error, int code) {
        deliver(request, () -> publish(new UiState(ExplorerViewModel.Status.ERROR, error, code, current.report)));
    }

    private void deliver(long request, Runnable update) {
        synchronized (deliveryLock) {
            if (!cleared) {
                mainHandler.post(() -> {
                    if (!cleared && request == generation) {
                        task = null;
                        update.run();
                    }
                });
            }
        }
    }

    private void publish(UiState value) {
        current = value;
        state.setValue(value);
    }

    /** Cancela o trabalho e impede callbacks tardios quando o detalhe é fechado definitivamente. */
    @Override
    protected void onCleared() {
        synchronized (deliveryLock) {
            cleared = true;
            mainHandler.removeCallbacksAndMessages(null);
        }
        generation++;
        if (task != null) {
            task.cancel(true);
        }
        executor.shutdownNow();
        super.onCleared();
    }
}
