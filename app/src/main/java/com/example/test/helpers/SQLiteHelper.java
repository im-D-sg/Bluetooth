package com.example.test.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SQLiteHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "database.db";
    private final Context context;
    public static final int VERSION = 1;

    public static final String TB_DEVICE = "tb_device";
    public static final String TB_SENSING = "tb_sensing";
    public static final String TB_DEEP_LEARNING = "tb_deep_learning";
    private static final String COL_DUID = "device_uid";
    private static final String COL_SENSOR_DATA = "sensor_data";
    private static final String COL_TIMESTAMP = "timestamp";

    public SQLiteHelper(@Nullable Context context) {
        super(context, DB_NAME, null, VERSION);
        this.context = context;
        try {
            createDatabase();
        } catch (IOException e) {
            throw new RuntimeException("Error creating database", e);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // 테이블 생성 코드 생략 (에셋에서 복사된 DB를 사용하므로)
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // DB 업그레이드 설정 생략 (에셋에서 복사된 DB를 사용하므로)
    }

    public void createDatabase() throws IOException {
        boolean dbExist = checkDatabase();
        if (!dbExist) {
            this.getReadableDatabase();
            try {
                copyDatabase();
            } catch (IOException e) {
                throw new Error("Error copying database");
            }
        }
    }

    private boolean checkDatabase() {
        SQLiteDatabase checkDB = null;
        try {
            String path = context.getDatabasePath(DB_NAME).getPath();
            checkDB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        } catch (Exception e) {
            // 데이터베이스가 존재하지 않음
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null;
    }

    private void copyDatabase() throws IOException {
        InputStream input = context.getAssets().open(DB_NAME);
        String outFileName = context.getDatabasePath(DB_NAME).getPath();
        OutputStream output = new FileOutputStream(outFileName);

        byte[] buffer = new byte[1024];
        int length;
        while ((length = input.read(buffer)) > 0) {
            output.write(buffer, 0, length);
        }

        output.flush();
        output.close();
        input.close();
    }

    public Cursor getDeviceUids() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TB_DEVICE, new String[]{COL_DUID}, null, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public void insertSensorData(int deviceIdx, int middleFlexSensor, int middlePressureSensor, int ringFlexSensor, int ringPressureSensor, int pinkyFlexSensor, int acceleration, int gyroscope, int magneticField) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("device_idx", deviceIdx);
        values.put("middle_flex_sensor", middleFlexSensor);
        values.put("middle_pressure_sensor", middlePressureSensor);
        values.put("ring_flex_sensor", ringFlexSensor);
        values.put("ring_pressure_sensor", ringPressureSensor);
        values.put("pinky_flex_sensor", pinkyFlexSensor);
        values.put("acceleration", acceleration);
        values.put("gyroscope", gyroscope);
        values.put("magnetic_field", magneticField);
        try {
            db.insertOrThrow(TB_SENSING, null, values);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    private int getNextModelIdx() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(model_idx) FROM " + TB_DEEP_LEARNING, null);
        int maxIdx = 0;
        if (cursor.moveToFirst()) {
            maxIdx = cursor.getInt(0);
        }
        cursor.close();
        return maxIdx + 1;
    }

    public void insertDeepLearningResult(String modelName, String analysisResult, float predictionRate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("model_idx", getNextModelIdx());
        values.put("model_name", modelName);
        values.put("analysis_result", analysisResult);
        values.put("prediction_rate", predictionRate);

        // 현재 날짜와 시간을 생성하여 create_at 컬럼에 추가
        String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put("created_at", currentDate);

        try {
            db.insertOrThrow(TB_DEEP_LEARNING, null, values);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    public int getRowCount(String tableName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public boolean hasTrainedModel() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TB_DEEP_LEARNING, null);
        boolean hasModel = false;
        if (cursor.moveToFirst()) {
            hasModel = cursor.getInt(0) > 0;
        }
        cursor.close();
        return hasModel;
    }
}
