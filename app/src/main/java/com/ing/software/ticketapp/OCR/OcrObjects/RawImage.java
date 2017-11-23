package com.ing.software.ticketapp.OCR.OcrObjects;

import android.graphics.Bitmap;

import com.ing.software.ticketapp.OCR.OcrUtils;

/**
 * Class to store only useful properties of source images
 * @author Michelon
 */

public class RawImage {

    private int height;
    private int width;
    private String grid;

    public RawImage(Bitmap bitmap) {
        height = bitmap.getHeight();
        width = bitmap.getWidth();
        grid = OcrUtils.getPreferredGrid(bitmap);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public String getGrid() {
        return grid;
    }
}
