package com.ing.software.ticketapp.OCR.OcrObjects;


import java.util.List;

/**
 * Class to store results from string search
 * @author Michelon
 */

public class RawStringResult {

    private RawText sourceText;
    private int distanceFromTarget;
    private List<RawText> detectedTexts = null;

    public RawStringResult(RawText rawText, int distanceFromTarget) {
        this.sourceText = rawText;
        this.distanceFromTarget = distanceFromTarget;
    }

    public void setDetectedTexts(List<RawText> detectedTexts) {
        this.detectedTexts = detectedTexts;
    }

    public RawText getSourceText() {
        return sourceText;
    }

    public int getDistanceFromTarget() {
        return distanceFromTarget;
    }

    public List<RawText> getDetectedTexts() {
        return detectedTexts;
    }
}
