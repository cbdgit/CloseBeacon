package se.bitsplz.closebeacon.service;


import com.koushikdutta.ion.Ion;
import java.util.concurrent.ExecutionException;
import se.bitsplz.closebeacon.activity.DeviceScanActivity;

/**
 * @author jonnakollin
 * @author j0na5L
 */
public final class HtmlHandler {

    public static String getPublicKey() {
        try {
            return Ion.with(DeviceScanActivity.getAppContext()).load("http://smartsensor.io/CBtest/getpubkey.php").asString().get().replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        } catch (InterruptedException e) {
            e.getMessage();
        } catch (ExecutionException e) {
            e.getMessage();
        }
        return "";
    }

    public static String validateLogin(String login) {
        try {
            return Ion.with(DeviceScanActivity.getAppContext()).load("http://smartsensor.io/CBtest/auth_user.php").addQuery("enc", login).asString().get().replace("=", "");
        } catch (InterruptedException e) {
            e.getMessage();
        } catch (ExecutionException e) {
            e.getMessage();
        }
        return "";
    }

    public static String activateBeacon(String activateReq) {
        try {
            return Ion.with(DeviceScanActivity.getAppContext()).load("http://smartsensor.io/CBtest/activate_beacon.php").addQuery("enc", activateReq).asString().get().replace("=", "");
        } catch (InterruptedException e) {
            e.getMessage();
        } catch (ExecutionException e) {
            e.getMessage();
        }
        return "";
    }
}
