package llc.ufwa.util;

import java.io.IOException;
import java.net.InetAddress;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

public final class AndroidNetworkUtil {


    /**
     * Current network is UMTS - aka GSM's 3G (AT&T)
     */
    private static final int NETWORK_TYPE_UMTS = 3;

    /**
     * Current network is EVDO revision B - aka CDMA's 3G (Verizon)
     */
    private static final int NETWORK_TYPE_EVDO_B = 12;

    /**
     * Verizon's 4G LTE (as seen on the Thunderbolt)
     */
    private static final int NETWORK_TYPE_4G_LTE = 13;

    /**
     * Verizon's 4G eHRPD 
     * http://developer.motorola.com/docstools/library/detecting-and-using-lte-networks/
     */
    private static final int NETWORK_TYPE_4G_EHRPD = 14;

    /**
     * The Default WiMAX data connection.  When active, all data traffic
     * will use this connection by default.  Should not coexist with other
     * default connections.
     */
    private static final int TYPE_WIMAX = 6;


    public static boolean isDataUp(final Context context) {
        // boolean logic trick, since sometimes android reports WIFI and MOBILE up at the same time
        return (isDataWIFIUp(context) != isDataMobileUp(context)) || isDataWiMAXUp(context);
    }

    public static boolean isDataMobileUp(final Context context) {
        ConnectivityManager connectivityManager = getConnectivityManager(context);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }

    public static boolean isData3GUp(final Context context) {
        ConnectivityManager connectivityManager = getConnectivityManager(context);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfo != null && networkInfo.isAvailable() && (networkInfo.getSubtype() == NETWORK_TYPE_UMTS || networkInfo.getSubtype() == NETWORK_TYPE_EVDO_B) && networkInfo.isConnected();
    }

    public static boolean isDataWIFIUp(final Context context) {
        ConnectivityManager connectivityManager = getConnectivityManager(context);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }

    public static boolean isDataWiMAXUp(final Context context) {
        ConnectivityManager connectivityManager = getConnectivityManager(context);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(TYPE_WIMAX);
        return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
    }

    public static boolean isData4GUp(final Context context) {
        ConnectivityManager connectivityManager = getConnectivityManager(context);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return networkInfo != null && networkInfo.isAvailable() && (networkInfo.getSubtype() == NETWORK_TYPE_4G_LTE || networkInfo.getSubtype() == NETWORK_TYPE_4G_EHRPD) && networkInfo.isConnected();
    }

    public static WifiManager getWifiManager(final Context context) {
        return (WifiManager) context.getSystemService(Application.WIFI_SERVICE);
    }

    public static InetAddress getMulticastInetAddress(final Context context) throws IOException {
        WifiManager wifi = getWifiManager(context);
        int intaddr = wifi.getConnectionInfo().getIpAddress();
        byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
        return InetAddress.getByAddress(byteaddr);
    }
    
    private static ConnectivityManager getConnectivityManager(final Context context) {
        return (ConnectivityManager) context.getSystemService(Application.CONNECTIVITY_SERVICE);
    }
}