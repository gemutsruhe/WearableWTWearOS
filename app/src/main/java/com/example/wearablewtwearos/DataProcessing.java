package com.example.wearablewtwearos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class DataProcessing {
    public static String trainingIdToImageName(String trainingId) {
        StringBuffer sbTrainingId = new StringBuffer(trainingId);
        sbTrainingId.replace(0, 1, sbTrainingId.substring(0, 1).toLowerCase());
        for(int k = 1; k < sbTrainingId.length(); k++) {
            if('A' <= sbTrainingId.charAt(k) && sbTrainingId.charAt(k) <= 'Z') {
                sbTrainingId.insert(k, '_');
                sbTrainingId.replace(k + 1, k + 2, sbTrainingId.substring(k + 1, k + 2).toLowerCase());
            }
        }
        String trainingImageName = sbTrainingId.toString();
        return trainingImageName;
    }

    public static Bitmap getTrainingImage(Context context, String trainingId) {
        String trainingImageName = DataProcessing.trainingIdToImageName(trainingId);
        int imageId = context.getResources().getIdentifier(trainingImageName, "drawable", context.getPackageName());
        Bitmap src = BitmapFactory.decodeResource(context.getResources(), imageId);
        Bitmap resized = Bitmap.createScaledBitmap(src, 120, 80, true);
        return resized;
    }
}
