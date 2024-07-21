package com.example.test.helpers;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeepLearningHelper {
    private static final String TAG = "DeepLearningHelper";
    private final SensorModel sensorModel;
    private final SQLiteHelper dbHelper;

    public DeepLearningHelper(Context context) throws IOException {
        this.sensorModel = new SensorModel(context);
        this.dbHelper = new SQLiteHelper(context);
    }

    public void predictAndSave() {
        try {
            List<int[]> sensorDataList = getSensorData();
            if (sensorDataList.isEmpty()) {
                Log.e(TAG, "No sensor data available for prediction.");
                return;
            }

            for (int[] inputData : sensorDataList) {
                float[] prediction = sensorModel.predict(inputData);

                // 예측 결과 저장
                String analysisResult = "Sensor data analyzed";
                float predictionRate = (prediction[0] + prediction[1]) / 2; // 두 예측의 평균
                dbHelper.insertDeepLearningResult("Sensor Model", analysisResult, predictionRate);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during prediction and saving", e);
        }
    }

    private List<int[]> getSensorData() {
        List<int[]> data = new ArrayList<>();
        Cursor cursor = dbHelper.getReadableDatabase().query(SQLiteHelper.TB_SENSING,
                new String[]{"middle_flex_sensor", "middle_pressure_sensor", "ring_flex_sensor", "ring_pressure_sensor", "pinky_flex_sensor"},
                null, null, null, null, null);

        while (cursor.moveToNext()) {
            int middleFlexSensor = cursor.getInt(0);
            int middlePressureSensor = cursor.getInt(1);
            int ringFlexSensor = cursor.getInt(2);
            int ringPressureSensor = cursor.getInt(3);
            int pinkyFlexSensor = cursor.getInt(4);
            data.add(new int[]{middleFlexSensor, middlePressureSensor, ringFlexSensor, ringPressureSensor, pinkyFlexSensor});
        }

        cursor.close();
        return data;
    }
}
