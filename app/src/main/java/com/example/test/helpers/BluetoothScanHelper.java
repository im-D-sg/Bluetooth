package com.example.test.helpers;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class BluetoothScanHelper {

    public static final String ACTION_DEVICE_FOUND = "com.example.test.ACTION_DEVICE_FOUND";
    public static final String EXTRA_DEVICE = "com.example.test.EXTRA_DEVICE";
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final SQLiteHelper dbHelper;
    private final Set<String> deviceUids;

    public BluetoothScanHelper(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.dbHelper = new SQLiteHelper(context);
        this.deviceUids = new HashSet<>();

        try {
            dbHelper.createDatabase();
        } catch (IOException e) {
            Toast.makeText(context, "데이터베이스를 생성하는 동안 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
        }

        loadDeviceUids();
    }

    private void loadDeviceUids() {
        Cursor cursor = dbHelper.getDeviceUids();
        if (cursor.moveToFirst()) {
            do {
                String deviceUid = cursor.getString(cursor.getColumnIndexOrThrow("device_uid"));
                deviceUids.add(deviceUid);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    String deviceUid = device.getAddress(); // MAC 주소를 UID로 사용
                    try {
                        if (deviceUids.contains(deviceUid)) {
                            Intent deviceFoundIntent = new Intent(ACTION_DEVICE_FOUND);
                            deviceFoundIntent.putExtra(EXTRA_DEVICE, device);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(deviceFoundIntent);
                        }
                    } catch (SecurityException e) {
                        Toast.makeText(context, "블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    public void startBluetoothDiscovery() {
        if (bluetoothAdapter == null) {
            Toast.makeText(context, "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (BluetoothPermissionHelper.checkAndRequestBluetoothPermissions((Activity) context)) {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                bluetoothAdapter.startDiscovery();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                context.registerReceiver(receiver, filter);
            }
        } catch (SecurityException e) {
            Toast.makeText(context, "블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopBluetoothDiscovery() {
        try {
            if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
            }
            context.unregisterReceiver(receiver);
        } catch (SecurityException e) {
            Toast.makeText(context, "블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
