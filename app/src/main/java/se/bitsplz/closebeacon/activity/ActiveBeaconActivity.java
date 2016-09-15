package se.bitsplz.closebeacon.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import se.bitsplz.closebeacon.request.ArrayByteBuilder;
import se.bitsplz.closebeacon.R;

/**
 * @author jonnakollin
 * @author j0na5L
 */
public class ActiveBeaconActivity extends Activity {

    private TextView majorText;
    private TextView minorText;
    private TextView uuidText;
    private TextView serialNumberText;
    private ArrayByteBuilder arrayByteBuilder;
    private String deviceName;
    private String deviceAddress;
    private String major;
    private String minor;
    private String uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_beacon);

        arrayByteBuilder = new ArrayByteBuilder();
        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(DeviceScanActivity.DEVICE_NAME);
        deviceAddress = intent.getStringExtra(DeviceScanActivity.DEVICE_ADDRESS);
        major = intent.getStringExtra(DeviceScanActivity.DEVICE_MAJOR);
        minor = intent.getStringExtra(DeviceScanActivity.DEVICE_MINOR);
        uuid = intent.getStringExtra(DeviceScanActivity.DEVICE_UUID);

        serialNumberText = (TextView) findViewById(R.id.serial_text);
        serialNumberText.setText(arrayByteBuilder.createSerialNumber(deviceAddress));

        getActionBar().setTitle(deviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);

        majorText = (TextView) findViewById(R.id.major);
        majorText.setText("Major: " + major);
        minorText = (TextView) findViewById(R.id.minor);
        minorText.setText("Minor: " + minor);
        uuidText = (TextView) findViewById(R.id.proximityuuid_text);
        uuidText.setText(uuid);

    }
}
