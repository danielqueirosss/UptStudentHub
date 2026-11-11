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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Gere a lista do Explorer com o padrão Executor + Handler + LiveData da Semana 6.
 */
public class ExplorerViewModel extends ViewModel {
    /** Estados que a Activity deverá traduzir em lista, progresso e mensagens. */
    public enum Status { IDLE, LOADING, SUCCESS, EMPTY, ERROR, CANCELLED }

    /** A Activity converte estes valores em recursos de texto; NONE indica ausência de erro. */
    public enum ErrorType { NONE, NO_CONNECTION, TIMEOUT, HTTP, PARSE }

    /** Estado imutável da lista, incluindo os dados anteriores e as definições que os produziram. */
    public static final class UiState {
        public final Status status;
        public final ErrorType error;
        public final int httpCode;
        public final Beach beach;
        public final ExplorerSettings settings;
        public final List<MarineHour> items;
        @Nullable public final Beach dataBeach;
        @Nullable public final ExplorerSettings dataSettings;

        private UiState(Status status, ErrorType error, int httpCode, Beach beach,
                        ExplorerSettings settings, List<MarineHour> items,
                        @Nullable Beach dataBeach, @Nullable ExplorerSettings dataSettings) {
            this.status = status;
            this.error = error;
            this.httpCode = httpCode;
            this.beach = beach;
            this.settings = settings;
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
            this.dataBeach = dataBeach;
            this.dataSettings = dataSettings;
        }
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private UiState current = new UiState(Status.IDLE, ErrorType.NONE, 0,
            Beach.MATOSINHOS, ExplorerSettings.defaults(), Collections.emptyList(), null, null);
    private final MutableLiveData<UiState> state = new MutableLiveData<>(current);
    private final MutableLiveData<Set<String>> favorites =
            new MutableLiveData<>(Collections.emptySet());
    private final Set<String> favoriteKeys = new HashSet<>();
    private Future<?> currentTask;
    // Só a main thread lê/altera a geração. O worker apenas transporta a sua cópia do número.
    private long requestGeneration;
    // Este monitor torna atómicos o post do worker e a limpeza de callbacks em onCleared().
    private final Object deliveryLock = new Object();
    private boolean cleared;
    private Boolean lastOnline;
    private boolean networkRecoveryPending;

    /** Memoriza a transição de rede sem guardar objetos Android no ViewModel. */
    @MainThread
    public void updateConnectivity(boolean online) {
        if (!online) networkRecoveryPending = false;
        else if (Boolean.FALSE.equals(lastOnline)
                && (current.status == Status.ERROR || current.status == Status.LOADING)) {
            networkRecoveryPending = true;
        }
        lastOnline = online;
    }

    /** Consome uma única tentativa de recuperação; espera se o pedido ainda estiver em curso. */
    @MainThread
    public boolean takeNetworkRecovery() {
        if (cleared || !Boolean.TRUE.equals(lastOnline) || !networkRecoveryPending
                || current.status != Status.ERROR) return false;
        networkRecoveryPending = false;
        return true;
    }

    /** Estado observável, sem expor MutableLiveData aos ecrãs. */
    public LiveData<UiState> getState() {
        return state;
    }

    /** Favoritos observáveis; cada conjunto publicado é uma cópia não modificável. */
    public LiveData<Set<String>> getFavorites() {
        return favorites;
    }

    /** Inicia apenas a primeira visita; a rotação não repete pedidos nem erros anteriores. */
    @MainThread
    public void loadIfNeeded() {
        if (!cleared && current.status == Status.IDLE) {
            startRequest(current.beach, current.settings);
        }
    }

    /** Atualiza a praia atual. Ignora cliques repetidos enquanto houver um pedido em curso. */
    @MainThread
    public void refresh() {
        if (!cleared && current.status != Status.LOADING) {
            startRequest(current.beach, current.settings);
        }
    }

    /** Muda de praia e carrega a lista; durante LOADING a escolha deve estar desativada. */
    @MainThread
    public void selectBeach(Beach beach) {
        if (!cleared && current.status != Status.LOADING && current.beach != beach) {
            startRequest(beach, current.settings);
        }
    }

    /**
     * Aplica o resultado validado das definições e recarrega a lista.
     * Substitui qualquer atualização anterior, para esta não publicar dados das opções antigas.
     */
    @MainThread
    public void applySettings(ExplorerSettings settings) {
        if (!cleared) {
            invalidateRequest();
            startRequest(current.beach, settings);
        }
    }

    /** Cancela imediatamente o estado visual e mantém os últimos dados disponíveis. */
    @MainThread
    public void cancel() {
        if (!cleared && current.status == Status.LOADING) {
            invalidateRequest();
            publish(retainData(Status.CANCELLED, ErrorType.NONE, 0));
        }
    }

    /** Consulta um favorito pela chave estável praia + data/hora, nunca pela posição da linha. */
    @MainThread
    public boolean isFavorite(String key) {
        return favoriteKeys.contains(key);
    }

    /** Aplica o resultado do detalhe; a Activity trata confirmação e Toast antes/depois. */
    @MainThread
    public void setFavorite(String key, boolean marked) {
        if (cleared) {
            return;
        }
        boolean changed = marked ? favoriteKeys.add(key) : favoriteKeys.remove(key);
        if (changed) {
            favorites.setValue(Collections.unmodifiableSet(new HashSet<>(favoriteKeys)));
        }
    }

    private void startRequest(Beach beach, ExplorerSettings settings) {
        final long generation = ++requestGeneration;
        Log.d("ExplorerRequest", "start #" + generation + " " + beach.id);
        UiState loading = new UiState(Status.LOADING, ErrorType.NONE, 0,
                beach, settings, current.items, current.dataBeach, current.dataSettings);
        // Guardar o Future antes de notificar observadores permite cancelar logo ao ver LOADING.
        currentTask = executor.submit(() -> fetch(generation, beach, settings));
        publish(loading);
    }

    private void fetch(long generation, Beach beach, ExplorerSettings settings) {
        try {
            String body = HttpClient.get(ExplorerApi.marineUrl(beach, settings));
            List<MarineHour> result = ExplorerParser.parseMarine(body, beach, settings);
            deliver(generation, () -> publish(new UiState(
                    result.isEmpty() ? Status.EMPTY : Status.SUCCESS,
                    ErrorType.NONE, 0, beach, settings, result, beach, settings)));
        } catch (HttpClient.HttpStatusException e) {
            deliverError(generation, ErrorType.HTTP, e.code);
        } catch (SocketTimeoutException e) {
            // SocketTimeoutException é uma InterruptedIOException, mas significa timeout.
            deliverError(generation, ErrorType.TIMEOUT, 0);
        } catch (InterruptedIOException e) {
            deliver(generation, () -> publish(retainData(Status.CANCELLED, ErrorType.NONE, 0)));
        } catch (IOException e) {
            deliverError(generation, ErrorType.NO_CONNECTION, 0);
        } catch (JSONException e) {
            deliverError(generation, ErrorType.PARSE, 0);
        }
    }

    private void deliverError(long generation, ErrorType error, int httpCode) {
        deliver(generation, () -> publish(retainData(Status.ERROR, error, httpCode)));
    }

    private void deliver(long generation, Runnable update) {
        synchronized (deliveryLock) {
            if (cleared) {
                return;
            }
            mainHandler.post(() -> {
                // Mesmo uma resposta já na fila é descartada se o pedido foi cancelado/substituído.
                if (!cleared && generation == requestGeneration) {
                    currentTask = null;
                    update.run();
                }
            });
        }
    }

    private UiState retainData(Status status, ErrorType error, int httpCode) {
        return new UiState(status, error, httpCode, current.beach, current.settings,
                current.items, current.dataBeach, current.dataSettings);
    }

    private void publish(UiState value) {
        if (value.status == Status.SUCCESS || value.status == Status.EMPTY || value.status == Status.CANCELLED) {
            networkRecoveryPending = false;
        }
        current = value;
        state.setValue(value); // Sempre na main thread.
    }

    private void invalidateRequest() {
        requestGeneration++;
        if (currentTask != null) {
            currentTask.cancel(true);
            currentTask = null;
        }
    }

    /** Liberta trabalho e callbacks ao fechar o ecrã definitivamente, não durante a rotação. */
    @Override
    protected void onCleared() {
        synchronized (deliveryLock) {
            cleared = true;
            mainHandler.removeCallbacksAndMessages(null);
        }
        invalidateRequest();
        executor.shutdownNow();
        super.onCleared();
    }
}
