package tn.esprit.myapplication.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** Simple connectivity helper used before network operations. */
public final class NetworkUtil {

    private NetworkUtil() {
        // Utility class
    }

    /**
     * Returns true when there is an active network with Wi-Fi, cellular, or ethernet transport.
     *
     * Note:
     *  - Callers typically use `!isOnline(context)` to gate network calls.
     *  - Lint complained this method is "always inverted", so we explicitly suppress that warning.
     */
    @SuppressLint("BooleanMethodIsAlwaysInverted")
    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        Network network = cm.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities cap = cm.getNetworkCapabilities(network);
        return cap != null
                && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}
