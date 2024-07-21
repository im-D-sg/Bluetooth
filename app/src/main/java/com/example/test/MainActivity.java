package com.example.test;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.test.helpers.BluetoothPermissionHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startBtn = findViewById(R.id.start_btn);

        // 블루투스 지원 여부 확인
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            // 장치가 블루투스를 지원하지 않는 경우
            Toast.makeText(this, "해당 기기에서는 블루투스 기능을 지원하지 않습니다.\n어플을 종료합니다.", Toast.LENGTH_LONG).show();
            finish(); // 어플 종료
            return;
        }

        // 버튼 클릭 이벤트 처리
        startBtn.setOnClickListener(v -> {
            // 블루투스 권한 확인 및 요청
            if (BluetoothPermissionHelper.checkAndRequestBluetoothPermissions(MainActivity.this)) {
                // 권한이 이미 있는 경우 ScanActivity로 이동
                startActivity(new Intent(MainActivity.this, ScanActivity.class));
            } else {
                // 권한이 없는 경우 권한 요청
                Toast.makeText(this, "블루투스 권한이 필요합니다. 권한을 요청합니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == BluetoothPermissionHelper.REQUEST_BLUETOOTH_PERMISSION) {
            boolean allPermissionsGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (allPermissionsGranted) {
                startActivity(new Intent(MainActivity.this, ScanActivity.class));
            } else {
                // 권한이 거부된 경우
                Toast.makeText(this, "해당 어플을 사용하려면 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                finish(); // 어플 종료
            }
        }
    }
}
