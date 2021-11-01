package com.example.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class ImageConvertUtils {

    public static String convertBitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 10, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        String result = Base64.getEncoder().encodeToString(bytes);

        return result;
    }

    public static Bitmap convertStringToBitmap(String strIn) {
        Bitmap bitmap = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(strIn);
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch(Exception e) {
            return null;
        }
        return bitmap;
    }

}
