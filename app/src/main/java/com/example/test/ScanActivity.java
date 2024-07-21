package com.example.test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.test.helpers.BluetoothPermissionHelper;
import com.example.test.helpers.BluetoothScanHelper;
import com.example.test.helpers.DeviceAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScanActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothScanHelper bluetoothScanHelper;
    private DeviceAdapter deviceAdapter;
    private List<BluetoothDevice> deviceList;

    private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothScanHelper.ACTION_DEVICE_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothScanHelper.EXTRA_DEVICE);
                if (device != null) {
                    deviceList.add(device);
                    deviceAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        deviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(deviceList, this::connectToDevice);
        recyclerView.setAdapter(deviceAdapter);

        if (BluetoothPermissionHelper.checkAndRequestBluetoothPermissions(this)) {
            startBluetoothScanning();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(deviceFoundReceiver,
                new IntentFilter(BluetoothScanHelper.ACTION_DEVICE_FOUND));
    }

    private void startBluetoothScanning() {
        bluetoothScanHelper = new BluetoothScanHelper(this);
        bluetoothScanHelper.startBluetoothDiscovery();
    }

    private void connectToDevice(BluetoothDevice device) {
        final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                // 블루투스 검색 중지
                if (BluetoothPermissionHelper.checkAndRequestBluetoothPermissions(this)) {
                    if (bluetoothAdapter.isDiscovering()) {
                        bluetoothAdapter.cancelDiscovery();
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(ScanActivity.this, "블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 소켓 생성 및 연결 시도
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                socket.connect();
                runOnUiThread(() -> {
                    Toast.makeText(ScanActivity.this, "연결 성공", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(ScanActivity.this, WorkActivity.class);
                    intent.putExtra("DEVICE_NAME", device.getName());
                    intent.putExtra("DEVICE_ADDRESS", device.getAddress());
                    startActivity(intent);
                    finish();
                });

            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(ScanActivity.this, "연결 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException closeException) {
                        runOnUiThread(() -> Toast.makeText(ScanActivity.this, "소켓 닫기 실패: " + closeException.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (SecurityException e) {
                runOnUiThread(() -> Toast.makeText(ScanActivity.this, "블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BluetoothPermissionHelper.REQUEST_BLUETOOTH_PERMISSION) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                startBluetoothScanning();
            } else {
                Toast.makeText(this, "블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothScanHelper != null) {
            bluetoothScanHelper.stopBluetoothDiscovery();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(deviceFoundReceiver);
    }
}
