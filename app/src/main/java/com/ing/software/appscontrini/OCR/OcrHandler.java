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
 * Temporary class to test analysis in OcrAnalyzer
 * @author Michelon
 */

public class OcrHandler extends Service {

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO use cloud files
        String testPic = "5.jpg";
        Bitmap test = getBitmapFromAsset(testPic);
        if (test==null) {
            Log.e("OcrHandler", "Received null image");
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

    /**
     * Get a bitmap from assets folder
     * @param strName name or path + name of the image (must be present)
     * @return bitmap of chosen path, null if nothing found
     */
    private Bitmap getBitmapFromAsset(String strName)
    {
        AssetManager assetManager = getAssets();
        InputStream istr = null;
        Bitmap bitmap = null;
        try {
            istr = assetManager.open(strName);
            bitmap = BitmapFactory.decodeStream(istr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                istr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }
}
