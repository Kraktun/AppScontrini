package com.ing.software.appscontrini.OCR;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */

public class OcrHandler extends Service {

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO use cloud files
        Bitmap test = getBitmapFromAsset("5.jpg");
        if (test!=null) {
            Log.e("ocrhandler", "image not null");
        }
        OcrAnalyzer.execute(test, this);
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return null;
    }

    private Bitmap getBitmapFromAsset(String strName)
    {
        AssetManager assetManager = getAssets();
        InputStream istr = null;
        try {
            istr = assetManager.open(strName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        return bitmap;
    }
}
