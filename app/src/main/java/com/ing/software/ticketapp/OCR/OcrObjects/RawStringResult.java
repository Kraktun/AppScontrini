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

    RawStringResult(RawText rawText, int distanceFromTarget) {
        this.sourceText = rawText;
        this.distanceFromTarget = distanceFromTarget;
    }

    public void addDetectedTexts(List<RawText> detectedTexts) {
        if (this.detectedTexts == null)
            this.detectedTexts = detectedTexts;
        else
            this.detectedTexts.addAll(detectedTexts);
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
