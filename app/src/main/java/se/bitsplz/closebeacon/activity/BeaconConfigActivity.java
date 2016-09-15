package se.bitsplz.closebeacon.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.UUID;

import se.bitsplz.closebeacon.request.ArrayByteBuilder;
import se.bitsplz.closebeacon.request.Encryption;
import se.bitsplz.closebeacon.service.BluetoothLeService;
import se.bitsplz.closebeacon.service.HtmlHandler;
import se.bitsplz.closebeacon.R;
import se.bitsplz.closebeacon.service.Storage;

/**
 * @author jonnakollin
 * @author j0na5L
 */
public class BeaconConfigActivity extends Activity {

    private final static String TAG = BeaconConfigActivity.class.getSimpleName();

    private String deviceName;

    private byte[] beaconActivateCommando;

    private EditText majorEditText;
    private EditText minorEditText;
    private EditText uuidEditText;
    private TextView serialNumberEditText;

    private ProgressDialog progressDialog;

    private String encrypted;
    private String activateResponse;

    private ArrayByteBuilder arrayByteBuilder;
    Encryption encryption;
    private String pubKey;

    private TextView dataValue;
    private TextView gattServicesDiscovered;
    private TextView connectionState;
    private String deviceAddress;
    private boolean isConnected = false;

    private BluetoothLeService bluetoothLeService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_config);
        arrayByteBuilder = new ArrayByteBuilder();
        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(DeviceScanActivity.DEVICE_NAME);
        deviceAddress = intent.getStringExtra(DeviceScanActivity.DEVICE_ADDRESS);
        connectionState = (TextView) findViewById(R.id.text_connection_state);
        serialNumberEditText = (TextView) findViewById(R.id.serialtext);
        serialNumberEditText.setText(arrayByteBuilder.createSerialNumber(deviceAddress));

        pubKey = Storage.readString(BeaconConfigActivity.this, "PublicKey", "");

        getActionBar().setTitle(deviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);

        majorEditText = (EditText) findViewById(R.id.majorText);
        minorEditText = (EditText) findViewById(R.id.minorText);
        uuidEditText = (EditText) findViewById(R.id.proximityuuid);

        encryption = new Encryption(pubKey);

    }


    @Override
    protected void onResume() {

        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());

        if (bluetoothLeService != null) {

            final boolean result = bluetoothLeService.connect(deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {

        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        if (bluetoothLeService != null) {
            unbindService(serviceConnection);
        }
        bluetoothLeService = null;
    }

    public void generateUUID(View view) {
        uuidEditText = (EditText) findViewById(R.id.proximityuuid);
        String uuid = UUID.randomUUID().toString();
        uuidEditText.setText(uuid);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.beacon_menu, menu);
        if (isConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                bluetoothLeService.connect(deviceAddress);
                return true;
            case R.id.menu_disconnect:
                bluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {

        if (data != null) {
            dataValue.setText(data);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {

        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.WRITE_CHARACTERISTIC);

        return intentFilter;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            if (!bluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            bluetoothLeService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {

                isConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));

                failedDialog("Success! ");
            }
        }
    };

    public void activateBeacon(View view) {

        String major = majorEditText.getText().toString();
        String minor = minorEditText.getText().toString();
        String uuid = uuidEditText.getText().toString();

        if (major.length() > 0 && minor.length() > 0 && uuid.length() > 0) {
            byte[] activate = arrayByteBuilder.activationBuilder(deviceAddress, major, minor, uuid);

            try {
                encrypted = encryption.encrypt(activate);
            } catch (Exception e) {
                e.printStackTrace();
            }
            String responseSha = HtmlHandler.activateBeacon(encrypted);
            if (responseSha.equals("FAILED")) {
                activateResponse = responseSha;
            } else {
                activateResponse = responseSha.replace("OK ", "");
                beaconActivateCommando = arrayByteBuilder.beaconActivationBuilder(activateResponse);

                writeCharacteristic(beaconActivateCommando);
            }
        }

    }


    private void writeCharacteristic(byte[] activationCommand) {
        final Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        if (bluetoothLeService != null) {

            final String characteristicUuid = "19721006-2004-2007-2014-acc0cbeac010";

            bluetoothLeService.connect(deviceAddress);
            bluetoothLeService.writeCharacteristic(DeviceScanActivity.SCAN_SERVICE_UUID, characteristicUuid, activationCommand);
        }
    }

    private void failedDialog(String data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Beacon activated");
        builder.setTitle(data);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}

