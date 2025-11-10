package tn.esprit.myapplication.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;

/** Simple connectivity helper used before network operations. */
public final class NetworkUtil {
    private NetworkUtil() {}

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        android.net.Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities cap = cm.getNetworkCapabilities(network);
        return cap != null && (cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}
