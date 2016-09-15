package se.bitsplz.closebeacon.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import se.bitsplz.closebeacon.request.ArrayByteBuilder;
import se.bitsplz.closebeacon.request.Encryption;
import se.bitsplz.closebeacon.service.HtmlHandler;
import se.bitsplz.closebeacon.R;
import se.bitsplz.closebeacon.service.Storage;

/**
 * @author jonnakollin
 * @author j0na5L
 */
public class DeviceScanActivity extends ListActivity {

    private static String TAG = "se.bitsplz.closebeacon";
    public static final String SCAN_SERVICE_UUID = "19721006-2004-2007-2014-acc0cbeac000";

    public static final String DEVICE_NAME = "se.bitsplz.closebeacon.DEVICE_NAME";
    public static final String DEVICE_MAJOR = "se.bitsplz.closebeacon.DEVICE_MAJOR";
    public static final String DEVICE_MINOR = "se.bitsplz.closebeacon.DEVICE_MINOR";
    public static final String DEVICE_UUID = "se.bitsplz.closebeacon.DEVICE_UUID";
    public static final String DEVICE_ADDRESS = "se.bitsplz.closebeacon.DEVICE_ADDRESS";

    private LeDeviceListAdapter leDeviceListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private ScanCallback scanCallback;
    private BluetoothGatt bluetoothGatt;

    private boolean isScanning;
    private Handler handler;
    private ArrayByteBuilder arrayByteBuilder;

    private String encrypted;
    private String ionResponse;
    private String pubKey;
    private Encryption encryption;
    private int counter;

    //private List<Beacon> beacons;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 5000;
    public static Context appContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
        handler = new Handler();
        arrayByteBuilder = new ArrayByteBuilder();
        appContext = getApplicationContext();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        pubKey = Storage.readString(DeviceScanActivity.this, "PublicKey", "");
        if (pubKey.isEmpty()) {
            pubKey = HtmlHandler.getPublicKey();
            Storage.writeToString(DeviceScanActivity.this, "PublicKey", pubKey);
        }
        encryption = new Encryption(pubKey);

        String stored = Storage.readString(DeviceScanActivity.this, "LoginKey", "");
        if (stored.equals("failed")) {
            failedDialog();
        } else if (stored.length() == 12) {
            Toast.makeText(DeviceScanActivity.this, "Already authenticated", Toast.LENGTH_SHORT).show();
        } else {
            loginDialog();
        }

    }

    private void loginDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View view = layoutInflater.inflate(R.layout.login_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setTitle("Login");
        builder.setCancelable(false);

        final EditText passwordText = (EditText) view.findViewById(R.id.login_password);

        builder.setPositiveButton("Submit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                loginCheck(passwordText.getText().toString());
                if (ionResponse.equals(arrayByteBuilder.getOkAuthResponse())) {
                    Toast.makeText(DeviceScanActivity.this, "Login Ok", Toast.LENGTH_LONG).show();
                    Storage.writeToString(DeviceScanActivity.this, "LoginKey", passwordText.getText().toString());
                } else {
                    counter++;
                    Toast.makeText(DeviceScanActivity.this, "Incorrect password", Toast.LENGTH_LONG).show();
                    if (counter < 3) {
                        loginDialog();
                    } else {
                        failedDialog();
                        Storage.writeToString(DeviceScanActivity.this, "LoginKey", "failed");
                    }
                }
            }
        });
        builder.setNegativeButton("Autocomplete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                loginCheck("dOH62cAsvkq5");
                if (ionResponse.equals(arrayByteBuilder.getOkAuthResponse())) {
                    Toast.makeText(DeviceScanActivity.this, "Login ok", Toast.LENGTH_LONG).show();
                    Storage.writeToString(DeviceScanActivity.this, "LoginKey", "dOH62cAsvkq5");
                } else {
                    counter++;
                    Toast.makeText(DeviceScanActivity.this, "Incorrect password", Toast.LENGTH_LONG).show();
                    if (counter < 3) {
                        loginDialog();
                    } else {
                        failedDialog();
                        Storage.writeToString(DeviceScanActivity.this, "LoginKey", "failed");

                    }
                }
            }

        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void failedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Failed to login");
        builder.setMessage("Reinstall app to try again");
        builder.setCancelable(false);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void loginCheck(String authKey) {
        try {
            encrypted = encryption.encrypt(arrayByteBuilder.authBuilder(authKey));
            ionResponse = HtmlHandler.validateLogin(encrypted);
        } catch (Exception e) {
            e.getMessage();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (!isScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                leDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        leDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(leDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        leDeviceListAdapter.clear();
    }

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final ScanResult scanResult = leDeviceListAdapter.getResult(position);
        if (scanResult == null) {
            return;
        }

        final BluetoothDevice btDevice = scanResult.getDevice();


        byte[] beaconBytes = scanResult.getScanRecord().getManufacturerSpecificData(76);

        if (beaconBytes != null) {
            int major = (beaconBytes[18] & 0xff) * 0x100 + (beaconBytes[19] & 0xff);
            int minor = (beaconBytes[20] & 0xff) * 0x100 + (beaconBytes[21] & 0xff);
            byte[] uuidArray = ByteBuffer.allocate(16).put(beaconBytes, 2, 16).array();
            String beaconUuid = getGuidFromByteArray(uuidArray);

            final Intent activeIntent = new Intent(this, ActiveBeaconActivity.class);
            activeIntent.putExtra(DeviceScanActivity.DEVICE_MAJOR, String.valueOf(major));
            activeIntent.putExtra(DeviceScanActivity.DEVICE_MINOR, String.valueOf(minor));
            activeIntent.putExtra(DeviceScanActivity.DEVICE_UUID, beaconUuid);
            activeIntent.putExtra(DeviceScanActivity.DEVICE_ADDRESS, scanResult.getDevice().getAddress());
            activeIntent.putExtra(DeviceScanActivity.DEVICE_NAME, scanResult.getDevice().getName());
            if (isScanning) {
                bluetoothLeScanner.stopScan(scanCallback);
                isScanning = false;
            }
            startActivity(activeIntent);
        } else {
            final Intent intent = new Intent(this, BeaconConfigActivity.class);
            intent.putExtra(DeviceScanActivity.DEVICE_NAME, scanResult.getDevice().getName());
            intent.putExtra(DeviceScanActivity.DEVICE_ADDRESS, scanResult.getDevice().getAddress());

            if (isScanning) {
                bluetoothLeScanner.stopScan(scanCallback);
                isScanning = false;
            }
            startActivity(intent);
            //connectToDevice(btDevice);

        }


    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothLeScanner.stopScan(scanCallback);
                    leDeviceListAdapter.notifyDataSetChanged();
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            isScanning = true;
            scanCallback = new SampleScanCallback();
            bluetoothLeScanner.startScan(scanFilter(), scanSettings(), scanCallback);
        } else {
            isScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
        invalidateOptionsMenu();
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<ScanResult> leResults;
        private LayoutInflater layoutInflater;

        public LeDeviceListAdapter() {
            super();
            leResults = new ArrayList<>();
            layoutInflater = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addResult(ScanResult result) {
            if (!leResults.contains(result)) {
                leResults.add(result);
            }
        }

        public ScanResult getResult(int position) {
            return leResults.get(position);
        }

        public void clear() {
            leResults.clear();
        }

        @Override
        public int getCount() {
            return leResults.size();
        }

        @Override
        public Object getItem(int i) {
            return leResults.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;

            if (view == null) {
                view = layoutInflater.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.bluetooth_device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.bluetooth_device_name);
                viewHolder.deviceRSSI = (TextView) view.findViewById(R.id.bluetooth_device_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            ScanResult scanResult = leResults.get(i);
            int rssi = scanResult.getRssi();

            viewHolder.deviceRSSI.setText("" + rssi);
            if (rssi <= -70) {
                viewHolder.deviceRSSI.setTextColor(Color.RED);
            } else {
                viewHolder.deviceRSSI.setTextColor(Color.GREEN);
            }

            final String deviceName = scanResult.getDevice().getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAddress.setText(scanResult.getDevice().getAddress());


            return view;
        }
    }

    private List<ScanFilter> scanFilter() {
        List<ScanFilter> scanFilters = new ArrayList<>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setDeviceName("closebeacon.com");
        //builder.setServiceUuid(ParcelUuid.fromString("19721006-2004-2007-2014-acc0cbeac000"));
        scanFilters.add(builder.build());

        return scanFilters;
    }

    private ScanSettings scanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }


    private class SampleScanCallback extends ScanCallback {
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                leDeviceListAdapter.addResult(result);
            }
            leDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            byte[] beaconBytes = result.getScanRecord().getManufacturerSpecificData(76);

            if (beaconBytes != null) {
                int major = (beaconBytes[18] & 0xff) * 0x100 + (beaconBytes[19] & 0xff);
                int minor = (beaconBytes[20] & 0xff) * 0x100 + (beaconBytes[21] & 0xff);
                Log.i("ManuArray", "Major: " + major + " Minor: " + minor);
                byte[] uuidArray = ByteBuffer.allocate(16).put(beaconBytes, 2, 16).array();
                String beaconUuid = getGuidFromByteArray(uuidArray);
                Log.i("UUIDArray", "" + beaconUuid);

            }


            final BluetoothDevice btDevice = result.getDevice();
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            Log.i("** RECORD1 **", String.valueOf(result.getScanRecord()));
            Log.i("** RECORD2 **", String.valueOf(result.getScanRecord().getManufacturerSpecificData()));

            leDeviceListAdapter.addResult(result);
            leDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(DeviceScanActivity.this, "Scan failed with error: " + errorCode, Toast.LENGTH_LONG)
                    .show();
        }
    }

    public static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRSSI;
    }

    public static String getGuidFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        return uuid.toString();
    }
}
