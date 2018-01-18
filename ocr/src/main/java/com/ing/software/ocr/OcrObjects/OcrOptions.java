package com.ing.software.ocr.OcrObjects;

/**
 * Object passed to the manager to avoid performing unnecessary operations
 * and consequently reduce time (time depends primarily on precision)
 */

public class OcrOptions {

    private boolean findTotal = true;
    private boolean findDate = true;
    private boolean findProducts = true;
    private int precision = 2; //Precision varies from 0 to 5, where 5 = max precision

    public OcrOptions(boolean findTotal, boolean findDate, boolean findProducts, int precision) {
        this.findTotal = findTotal;
        this.findDate = findDate;
        this.findProducts = findProducts;
        this.precision = precision;
    }

    public static OcrOptions getDefaultOptions() {
        return new OcrOptions(true, true, true, 2);
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public void setFindTotal(boolean findTotal) {
        this.findTotal = findTotal;
    }

    public void setFindDate(boolean findDate) {
        this.findDate = findDate;
    }

    public void setFindProducts(boolean findProducts) {
        this.findProducts = findProducts;
    }

    public boolean isFindTotal() {
        return findTotal;
    }

    public boolean isFindDate() {
        return findDate;
    }

    public boolean isFindProducts() {
        return findProducts;
    }

    public int getPrecision() {
        return precision;
    }
}
