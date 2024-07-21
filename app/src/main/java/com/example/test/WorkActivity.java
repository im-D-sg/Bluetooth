package com.example.test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.test.helpers.BluetoothPermissionHelper;
import com.example.test.helpers.DeepLearningHelper;
import com.example.test.helpers.SQLiteHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class WorkActivity extends AppCompatActivity {

    private static final String TAG = "WorkActivity";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private ConnectedThread connectedThread;
    private SQLiteHelper dbHelper;
    private DeepLearningHelper deepLearningHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work);

        // Intent로 전달받은 기기 정보 저장
        String deviceAddress = getIntent().getStringExtra("DEVICE_ADDRESS");

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            dbHelper = new SQLiteHelper(this);
            deepLearningHelper = new DeepLearningHelper(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 학습된 모델이 있는지 확인
        if (dbHelper.hasTrainedModel()) {
            navigateToRunningActivity(deviceAddress);
        } else {
            // 권한 확인 및 요청
            if (BluetoothPermissionHelper.checkAndRequestBluetoothPermissions(this)) {
                connectToDevice(device);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BluetoothPermissionHelper.REQUEST_BLUETOOTH_PERMISSION) {
            // 권한이 부여되었는지 확인
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String deviceAddress = getIntent().getStringExtra("DEVICE_ADDRESS");
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
                connectToDevice(device);
            } else {
                Toast.makeText(this, "블루투스 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            connectedThread = new ConnectedThread(bluetoothSocket);
            connectedThread.start();
        } catch (IOException e) {
            Log.e(TAG, "Error connecting to device", e);
            Toast.makeText(this, "기기 연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
            closeSocket();
        }
    }

    private void closeSocket() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        closeSocket();
    }

    private void performDeepLearning() {
        deepLearningHelper.predictAndSave();
        // 딥러닝이 완료되면 RunningActivity로 이동
        String deviceAddress = getIntent().getStringExtra("DEVICE_ADDRESS");
        navigateToRunningActivity(deviceAddress);
    }

    private void navigateToRunningActivity(String deviceAddress) {
        Intent intent = new Intent(WorkActivity.this, RunningActivity.class);
        intent.putExtra("DEVICE_ADDRESS", deviceAddress);
        startActivity(intent);
        finish(); // WorkActivity를 종료하여 메모리 해제
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final Handler handler;
        private final Handler dataHandler;
        private final Runnable dataSaverRunnable;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            HandlerThread handlerThread = new HandlerThread("BluetoothHandlerThread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper(), new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    String readMessage = (String) msg.obj;
                    // 수신한 데이터를 데이터베이스에 저장
                    String[] sensorData = readMessage.split(","); // assuming sensor data is comma-separated
                    if (sensorData.length == 9) {
                        try {
                            int deviceIdx = Integer.parseInt(sensorData[0]);
                            int middleFlexSensor = Integer.parseInt(sensorData[1]);
                            int middlePressureSensor = Integer.parseInt(sensorData[2]);
                            int ringFlexSensor = Integer.parseInt(sensorData[3]);
                            int ringPressureSensor = Integer.parseInt(sensorData[4]);
                            int pinkyFlexSensor = Integer.parseInt(sensorData[5]);
                            int acceleration = Integer.parseInt(sensorData[6]);
                            int gyroscope = Integer.parseInt(sensorData[7]);
                            int magneticField = Integer.parseInt(sensorData[8]);
                            dbHelper.insertSensorData(deviceIdx, middleFlexSensor, middlePressureSensor, ringFlexSensor, ringPressureSensor, pinkyFlexSensor, acceleration, gyroscope, magneticField);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing sensor data", e);
                        }
                    }

                    // 데이터베이스 행 갯수 확인 후 딥러닝 수행
                    if (dbHelper.getRowCount(SQLiteHelper.TB_SENSING) >= 12000) {
                        dataHandler.removeCallbacks(dataSaverRunnable); // 데이터 수신 중단
                        performDeepLearning(); // 딥러닝 수행
                    }
                    return true;
                }
            });

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error getting input stream", e);
            }
            mmInStream = tmpIn;

            dataHandler = new Handler(handlerThread.getLooper());
            dataSaverRunnable = new Runnable() {
                @Override
                public void run() {
                    readAndSaveData();
                    dataHandler.postDelayed(this, 50); // 50ms delay for 20 times per second
                }
            };
        }

        public void run() {
            dataHandler.post(dataSaverRunnable);
        }

        private void readAndSaveData() {
            byte[] buffer = new byte[1024];
            int bytes;

            try {
                bytes = mmInStream.read(buffer);
                if (bytes > 0) {
                    String readMessage = new String(buffer, 0, bytes);
                    handler.obtainMessage(0, readMessage).sendToTarget();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading from input stream", e);
            }
        }

        public void cancel() {
            try {
                mmInStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing input stream", e);
            }
            dataHandler.removeCallbacks(dataSaverRunnable);
        }
    }
}
