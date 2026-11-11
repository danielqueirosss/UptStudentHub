package pt.upt.studenthub;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

/** Eventos do dispositivo associados à Activity; não executa HTTP nem sobrevive ao ecrã. */
final class ExplorerDeviceEvents {
    private final AppCompatActivity activity;
    private final ExplorerViewModel viewModel;
    private final ConnectivityManager connectivity;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final View offlineBanner, batteryBanner;
    private ConnectivityManager.NetworkCallback callback;
    private Network activeNetwork;
    private boolean started, online, batteryLow, scheduled;
    private long interval;

    ExplorerDeviceEvents(AppCompatActivity activity, ExplorerViewModel viewModel) {
        this.activity = activity;
        this.viewModel = viewModel;
        connectivity = ContextCompat.getSystemService(activity, ConnectivityManager.class);
        offlineBanner = activity.findViewById(R.id.tvExplorerOffline);
        batteryBanner = activity.findViewById(R.id.tvExplorerBattery);
    }

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        /** Trata o evento do sistema na main thread, sem executar pedidos de rede. */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!started) return;
            if (Intent.ACTION_BATTERY_LOW.equals(intent.getAction())) setBatteryLow(true);
            else if (Intent.ACTION_BATTERY_OKAY.equals(intent.getAction())) setBatteryLow(false);
        }
    };

    private final Runnable periodic = new Runnable() {
        /** Executa uma iteração do ciclo e agenda a próxima, respeitando o estado do ecrã. */
        @Override
        public void run() {
            scheduled = false;
            if (!started || batteryLow || !online) return;
            ExplorerViewModel.UiState state = viewModel.getState().getValue();
            if (state != null && state.status != ExplorerViewModel.Status.LOADING) {
                Log.d("ExplorerDevice", "periodic refresh");
                viewModel.refresh();
            }
            schedule();
        }
    };

    private final Runnable recovery = this::recoverNetwork;

    private void recoverNetwork() {
        if (started && online && viewModel.takeNetworkRecovery()) {
            Log.d("ExplorerDevice", "network recovery refresh");
            viewModel.refresh();
        }
    }

    void start() {
        if (started) return;
        started = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_LOW);
        filter.addAction(Intent.ACTION_BATTERY_OKAY);
        ContextCompat.registerReceiver(activity, batteryReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
        // LOW/OKAY não são sticky: consultar a bateria cobre eventos ocorridos com o ecrã fechado.
        Intent battery = activity.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        boolean low = false;
        if (battery != null) {
            if (Build.VERSION.SDK_INT >= 28) low = battery.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false);
            else {
                int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                low = level >= 0 && scale > 0 && level * 100L <= scale * 15L
                        && battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) == 0;
            }
        }
        setBatteryLow(low);
        activeNetwork = connectivity.getActiveNetwork();
        NetworkCapabilities initial = activeNetwork == null ? null : connectivity.getNetworkCapabilities(activeNetwork);
        setOnline(hasInternet(initial));
        callback = new ConnectivityManager.NetworkCallback() {
            private void dispatch(Runnable update) {
                activity.runOnUiThread(() -> {
                    if (started && ExplorerDeviceEvents.this.callback == this) update.run();
                });
            }
            /** Encaminha a disponibilidade da rede para a main thread. */
            @Override public void onAvailable(@NonNull Network network) {
                dispatch(() -> {
                    activeNetwork = network;
                    // Desde API 26, onCapabilitiesChanged segue sempre onAvailable.
                    if (Build.VERSION.SDK_INT < 26) setOnline(true);
                });
            }
            /** Encaminha a perda da rede para a main thread. */
            @Override public void onLost(@NonNull Network network) {
                dispatch(() -> {
                    if (network.equals(activeNetwork)) {
                        activeNetwork = null;
                        setOnline(false);
                    }
                });
            }
            /** Processa as capacidades fornecidas pelo sistema sem consultar uma versão anterior da rede. */
            @Override public void onCapabilitiesChanged(@NonNull Network network,
                    @NonNull NetworkCapabilities capabilities) {
                boolean available = hasInternet(capabilities);
                dispatch(() -> {
                    if (network.equals(activeNetwork)) setOnline(available);
                });
            }
        };
        connectivity.registerDefaultNetworkCallback(callback);
        Log.d("ExplorerDevice", "start: receiver and callback registered");
    }

    void stop() {
        if (!started) return;
        started = false;
        handler.removeCallbacksAndMessages(null);
        scheduled = false;
        activity.unregisterReceiver(batteryReceiver);
        connectivity.unregisterNetworkCallback(callback);
        callback = null;
        Log.d("ExplorerDevice", "stop: callbacks and timer removed");
    }

    private boolean hasInternet(NetworkCapabilities caps) {
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void setOnline(boolean available) {
        online = available;
        Log.d("ExplorerDevice", online ? "network online" : "network offline");
        offlineBanner.setVisibility(online ? View.GONE : View.VISIBLE);
        viewModel.updateConnectivity(online);
        stateChanged();
    }

    private void setBatteryLow(boolean low) {
        batteryLow = low;
        batteryBanner.setVisibility(low ? View.VISIBLE : View.GONE);
        Log.d("ExplorerDevice", low ? "battery low: periodic suspended" : "battery okay");
        schedule();
    }

    void stateChanged() {
        if (!started) return;
        // Esperar pelo fim de um pedido que ainda estava em curso quando a rede regressou.
        handler.removeCallbacks(recovery);
        handler.post(recovery);
        schedule();
    }

    private void schedule() {
        ExplorerViewModel.UiState state = viewModel.getState().getValue();
        long nextInterval = state == null ? 0 : state.settings.refreshIntervalMillis();
        if (!started || batteryLow || !online || interval != nextInterval) {
            handler.removeCallbacks(periodic);
            scheduled = false;
        }
        interval = nextInterval;
        if (started && online && !batteryLow && interval > 0 && !scheduled) {
            scheduled = true;
            handler.postDelayed(periodic, interval);
            Log.d("ExplorerDevice", "timer scheduled: " + interval + " ms");
        }
    }
}
