// ZeroTermux add File
package com.termux.x11;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * Static receiver for internal-channel startup.
 *
 * Upstream assumes the host activity is already alive and has registered a runtime receiver.
 * ZeroTermux embeds X11 inside the Termux activity, so the runtime receiver may not exist yet
 * when `termux-x11` is started from the shell. This bridge keeps the latest connection binder
 * until the embedded MainActivity is initialized.
 */
public class CmdEntryPointStartReceiver extends BroadcastReceiver {
    private static final Object LOCK = new Object();
    private static Bundle pendingConnection;

    static void consumePendingConnection(MainActivity activity) {
        if (activity == null) {
            return;
        }

        Bundle bundle;
        synchronized (LOCK) {
            bundle = pendingConnection;
            pendingConnection = null;
        }

        if (bundle == null) {
            return;
        }

        Intent intent = new Intent(CmdEntryPoint.ACTION_START);
        intent.putExtra(null, bundle);
        Log.d("SurfaceChangedListener", " consume pending ACTION_START intent");
        activity.onReceiveConnection(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !CmdEntryPoint.ACTION_START.equals(intent.getAction())) {
            return;
        }

        Bundle bundle = intent.getBundleExtra(null);
        IBinder binder = bundle == null ? null : bundle.getBinder(null);
        if (binder == null) {
            Log.w("SurfaceChangedListener", " ACTION_START received without binder payload");
            return;
        }

        MainActivity instance = MainActivity.getInstance();
        if (instance != null) {
            Log.d("SurfaceChangedListener", " deliver ACTION_START directly to active MainActivity");
            instance.onReceiveConnection(intent);
            return;
        }

        synchronized (LOCK) {
            pendingConnection = new Bundle();
            pendingConnection.putBinder(null, binder);
        }
        Log.d("SurfaceChangedListener", " cached ACTION_START until MainActivity init");
    }
}
