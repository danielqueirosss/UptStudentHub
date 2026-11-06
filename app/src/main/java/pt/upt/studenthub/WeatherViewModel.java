package pt.upt.studenthub;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.json.JSONException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
/**
 * Week 7: same architecture as Week 6 (Executor + Handler + LiveData),
 * but the slow work is now a REAL HTTP request.
 * Rule: no Activity, View or Context here — errors are reported as an enum.
 */
public class WeatherViewModel extends ViewModel {
    public enum Status { IDLE, LOADING, SUCCESS, ERROR }
    public enum ErrorType { NO_CONNECTION, TIMEOUT, HTTP, PARSE }
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Status> status = new MutableLiveData<>(Status.IDLE);
    private final MutableLiveData<WeatherReport> report = new MutableLiveData<>();
    private final MutableLiveData<ErrorType> error = new MutableLiveData<>();
    private int lastHttpCode; // only written on the main thread
    private Future<?> currentTask;
    public LiveData<Status> getStatus() { return status; }
    public LiveData<WeatherReport> getReport() { return report; }
    public LiveData<ErrorType> getError() { return error; }
    public int getLastHttpCode() { return lastHttpCode; }
    /** Called on the UI thread. Downloads and parses the weather on the worker thread. */
    public void load(String url) {
        if (status.getValue() == Status.LOADING) {
            return; // one request at a time
        }
        status.setValue(Status.LOADING);
        currentTask = executor.submit(() -> {
            // ---- WORKER THREAD ----
            try {
                String body = HttpClient.get(url); // network (blocking)
                WeatherReport result = WeatherParser.parse(body); // JSON -> object
                mainHandler.post(() -> { // ---- UI THREAD ----
                    report.setValue(result);
                    status.setValue(Status.SUCCESS);
                });
            } catch (HttpClient.HttpStatusException e) { // server answered 4xx/5xx
                postError(ErrorType.HTTP, e.code);
            } catch (SocketTimeoutException e) { // server too slow
                postError(ErrorType.TIMEOUT, 0);
            } catch (IOException e) { // no network, DNS, refused...
                postError(ErrorType.NO_CONNECTION, 0);
            } catch (JSONException e) { // unexpected data format
                postError(ErrorType.PARSE, 0);
            }
        });
    }
    private void postError(ErrorType type, int httpCode) {
        mainHandler.post(() -> {
            lastHttpCode = httpCode;
            error.setValue(type);
            status.setValue(Status.ERROR); // the last good report (if any) is kept
        });
    }
    @Override
    protected void onCleared() {
        if (currentTask != null) {
            currentTask.cancel(true);
        }
        executor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onCleared();
    }
}
