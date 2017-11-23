package com.ing.software.ticketapp.OCR.OcrObjects;

import android.support.annotation.NonNull;

/**
 * Class to store results from grid search
 * @author Michelon
 */

public class RawGridResult implements Comparable<RawGridResult>{

    private int percentage;
    private RawText singleText;

    public RawGridResult(RawText singleText, int percentage) {
        this.percentage = percentage;
        this.singleText = singleText;
    }

    public int getPercentage() {
        return percentage;
    }

    public RawText getText() {
        return singleText;
    }

    @Override
    public int compareTo(@NonNull RawGridResult rawGridResult) {
        if (getPercentage() == rawGridResult.getPercentage())
            return getText().compareTo(rawGridResult.getText());
        else
            return  getPercentage() - rawGridResult.getPercentage();
    }
}
