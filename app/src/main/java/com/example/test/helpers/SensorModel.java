package com.example.test.helpers;

import android.content.Context;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class SensorModel {
    private Interpreter tflite;

    public SensorModel(Context context) throws IOException {
        tflite = new Interpreter(loadModelFile(context));
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(context.getAssets().openFd("sensor_pair_model.tflite").getFileDescriptor());
             FileChannel fileChannel = fileInputStream.getChannel()) {
            long startOffset = context.getAssets().openFd("sensor_pair_model.tflite").getStartOffset();
            long declaredLength = context.getAssets().openFd("sensor_pair_model.tflite").getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }

    public float[] predict(int[] input) {
        float[][] inputVal = new float[1][input.length];
        for (int i = 0; i < input.length; i++) {
            inputVal[0][i] = input[i];
        }
        float[][] output_1 = new float[1][1];
        float[][] output_2 = new float[1][1];

        Object[] inputs = {inputVal};
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, output_1);
        outputs.put(1, output_2);

        tflite.runForMultipleInputsOutputs(inputs, outputs);

        return new float[]{output_1[0][0], output_2[0][0]};
    }
}
