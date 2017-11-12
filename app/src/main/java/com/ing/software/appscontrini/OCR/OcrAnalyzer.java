package com.ing.software.appscontrini.OCR;

import android.app.Service;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Class containing different methods to analyze a picture
 * @author Michelon
 */

public class OcrAnalyzer {

    /**
     * Test method for analysis
     * @param photo source photo to analyze
     * @param service main service for the detector to run
     */
    static void execute(Bitmap photo, Service service) {
        Log.d("OcrAnalyzer.execute:" , "Starting analyzing" );
        analyzeSingleText(photo, service);
        String testString = "TOTALE";
        int testPrecision = 100;
        analyzeBruteFirstString(photo, service, testString);
        analyzeBruteContinuousString(photo, service, testString);
        analyzeBruteContHorizValue(photo, service, testString, testPrecision);
    }

    /**
     * Analyze a photo and returns everything it could decode
     * @param photo photo to analyze
     * @param service main service for the detector to run
     * @return string containing everything it found
     */
    static String analyzeSingleText(Bitmap photo, Service service) {
        List<TextBlock> orderedTextBlocks = initAnalysis(photo, service);
        Log.d("OcrAnalyzer.analyzeST:" , "Blocks detected");
        orderedTextBlocks = OCRUtils.orderBlocks(orderedTextBlocks);
        Log.d("OcrAnalyzer.analyzeST:" , "Blocks ordered");
        int[] borders = OCRUtils.getRectBorders(orderedTextBlocks, photo);
        int left = borders[0];
        int right = borders[2];
        int top = borders[1];
        int bottom = borders[3];
        Bitmap croppedPhoto = OCRUtils.cropImage(photo, left, top, right, bottom);
        String grid = OCRUtils.getPreferredGrid(croppedPhoto);
        Log.d("OcrAnalyzer.analyzeST:" , "Photo cropped");
        List<TextBlock> newOrderedTextBlocks = initAnalysis(croppedPhoto, service);
        newOrderedTextBlocks = OCRUtils.orderBlocks(newOrderedTextBlocks);
        Log.d("OcrAnalyzer.analyzeST:" , "New Blocks ordered");
        List<RawBlock> rawBlocks = new ArrayList<>();
        for (TextBlock textBlock : newOrderedTextBlocks) {
            rawBlocks.add(new RawBlock(textBlock, croppedPhoto, grid));
        }
        StringBuilder detectionList = new StringBuilder();
        for (RawBlock rawBlock : rawBlocks) {
            List<RawBlock.RawText> rawTexts = rawBlock.getRawTexts();
            for (RawBlock.RawText rawText : rawTexts) {
                detectionList.append(rawText.getDetection())
                        .append("\n");
            }
        }
        Log.d("OcrAnalyzer.analyzeST", "detected: "+ detectionList);
        return detectionList.toString();
    }

    /**
     * Search for first occurrence of a string in chosen photo. Order is top->bottom, left->right
     * @param photo photo to analyze
     * @param service main service for the detector to run
     * @param testString string to search
     * @return string containing first RawBlock.RawText where it found the string
     */
    static String analyzeBruteFirstString(Bitmap photo, Service service, String testString) {
        List<TextBlock> orderedTextBlocks = initAnalysis(photo, service);
        Log.d("OcrAnalyzer.analyzeBFS:" , "Blocks detected");
        orderedTextBlocks = OCRUtils.orderBlocks(orderedTextBlocks);
        Log.d("OcrAnalyzer.analyzeBFS:" , "Blocks ordered");
        int[] borders = OCRUtils.getRectBorders(orderedTextBlocks, photo);
        int left = borders[0];
        int right = borders[2];
        int top = borders[1];
        int bottom = borders[3];
        Bitmap croppedPhoto = OCRUtils.cropImage(photo, left, top, right, bottom);
        String grid = OCRUtils.getPreferredGrid(croppedPhoto);
        Log.d("OcrAnalyzer.analyzeBFS:" , "Photo cropped");
        List<TextBlock> newOrderedTextBlocks = initAnalysis(croppedPhoto, service);
        newOrderedTextBlocks = OCRUtils.orderBlocks(newOrderedTextBlocks);
        Log.d("OcrAnalyzer.analyzeBFS:" , "New Blocks ordered");
        List<RawBlock> rawBlocks = new ArrayList<>();
        for (TextBlock textBlock : newOrderedTextBlocks) {
            rawBlocks.add(new RawBlock(textBlock, croppedPhoto, grid));
        }
        RawBlock.RawText targetText = null;
        for (RawBlock rawBlock : rawBlocks) {
            targetText = rawBlock.bruteSearch(testString);
            if (targetText != null)
                break;
            }
        if (targetText != null) {
            Log.d("OcrAnalyzer.analyzeBFS", "Found first target string: "+ testString + " \nat: " + targetText.getDetection());
            Log.d("OcrAnalyzer.analyzeBFS", "Target text is at (left, top, right, bottom): "+ targetText.getRect().left + "; "
                    + targetText.getRect().top + "; " + targetText.getRect().right + "; "+ targetText.getRect().bottom + ".");
        }
        return targetText.getDetection();
    }

    /**
     * Search for all occurrences of a string in chosen photo. Order is top->bottom, left->right
     * @param photo photo to analyze
     * @param service main service for the detector to run
     * @param testString string to search
     * @return list of all RawBlock.RawText where it found the string
     */
    static List<RawBlock.RawText> analyzeBruteContinuousString(Bitmap photo, Service service, String testString) {
        List<TextBlock> orderedTextBlocks = initAnalysis(photo, service);
        Log.d("OcrAnalyzer.analyze:" , "Blocks detected");
        orderedTextBlocks = OCRUtils.orderBlocks(orderedTextBlocks);
        Log.d("OcrAnalyzer.analyze:" , "Blocks ordered");
        int[] borders = OCRUtils.getRectBorders(orderedTextBlocks, photo);
        int left = borders[0];
        int right = borders[2];
        int top = borders[1];
        int bottom = borders[3];
        Bitmap croppedPhoto = OCRUtils.cropImage(photo, left, top, right, bottom);
        String grid = OCRUtils.getPreferredGrid(croppedPhoto);
        Log.d("OcrAnalyzer.analyze:" , "Photo cropped");
        List<TextBlock> newOrderedTextBlocks = initAnalysis(croppedPhoto, service);
        newOrderedTextBlocks = OCRUtils.orderBlocks(newOrderedTextBlocks);
        Log.d("OcrAnalyzer.analyze:" , "New Blocks ordered");
        List<RawBlock> rawBlocks = new ArrayList<>();
        for (TextBlock textBlock : newOrderedTextBlocks) {
            rawBlocks.add(new RawBlock(textBlock, croppedPhoto, grid));
        }
        List<RawBlock.RawText> targetTextList = new ArrayList<>();
        for (RawBlock rawBlock : rawBlocks) {
            List<RawBlock.RawText> tempTextList;
            tempTextList = rawBlock.bruteSearchContinuous(testString);
            if (tempTextList != null) {
                for (int i = 0; i < tempTextList.size(); i++) {
                    targetTextList.add(tempTextList.get(i));
                }
            }
        }
        if (targetTextList.size() >0) {
            for (RawBlock.RawText text : targetTextList) {
                Log.d("OcrAnalyzer", "Found target string: " + testString + " \nat: " + text.getDetection());
                Log.d("OcrAnalyzer", "Target text is at (left, top, right, bottom): " + text.getRect().left
                        + "; " + text.getRect().top + "; " + text.getRect().right + "; " + text.getRect().bottom + ".");
            }
        }
        return targetTextList;
    }

    /**
     * Searches for all occurrences of a string in chosen photo, then retrieves also text with similar
     * distance from top and bottom. Max difference from string detection is defined by precision. Order is top->bottom, left->right
     * @param photo photo to analyze
     * @param service main service for the detector to run
     * @param testString string to search
     * @param precision int > 0 percentage of the height of detected RawText to extend. See RawBlock.extendRect().
     * @return list of all RawBlock.RawText where it found the string and with similar distance from top and bottom.
     */
    static List<RawBlock.RawText> analyzeBruteContHorizValue(Bitmap photo, Service service, String testString, int precision) {
        List<TextBlock> orderedTextBlocks = initAnalysis(photo, service);
        Log.d("OcrAnalyzer.analyze:" , "Blocks detected");
        orderedTextBlocks = OCRUtils.orderBlocks(orderedTextBlocks);
        Log.d("OcrAnalyzer.analyze:" , "Blocks ordered");
        int[] borders = OCRUtils.getRectBorders(orderedTextBlocks, photo);
        int left = borders[0];
        int right = borders[2];
        int top = borders[1];
        int bottom = borders[3];
        Bitmap croppedPhoto = OCRUtils.cropImage(photo, left, top, right, bottom);
        String grid = OCRUtils.getPreferredGrid(croppedPhoto);
        Log.d("OcrAnalyzer.analyze:" , "Photo cropped");
        List<TextBlock> newOrderedTextBlocks = initAnalysis(croppedPhoto, service);
        newOrderedTextBlocks = OCRUtils.orderBlocks(newOrderedTextBlocks);
        Log.d("OcrAnalyzer.analyze:" , "New Blocks ordered");
        List<RawBlock> rawBlocks = new ArrayList<>();
        for (TextBlock textBlock : newOrderedTextBlocks) {
            rawBlocks.add(new RawBlock(textBlock, croppedPhoto, grid));
        }
        List<RawBlock.RawText> targetTextList = new ArrayList<>();
        for (RawBlock rawBlock : rawBlocks) {
            List<RawBlock.RawText> tempTextList = new ArrayList<>();
            tempTextList = rawBlock.bruteSearchContinuous(testString);
            if (tempTextList != null) {
                for (int i = 0; i < tempTextList.size(); i++) {
                    targetTextList.add(tempTextList.get(i));
                }
            }
        }
        if (targetTextList.size() >0) {
            for (RawBlock.RawText text : targetTextList) {
                Log.d("OcrAnalyzer", "Found target string: " + testString + " \nat: " + text.getDetection());
                Log.d("OcrAnalyzer", "Target text is at (left, top, right, bottom): " + text.getRect().left + "; "
                        + text.getRect().top + "; " + text.getRect().right + "; " + text.getRect().bottom + ".");
            }
        }
        List<RawBlock.RawText> resultTexts = new ArrayList<>();
        for (RawBlock rawBlock : rawBlocks) {
            for (RawBlock.RawText rawText : targetTextList) {
                List<RawBlock.RawText> tempResultList = rawBlock.findByPosition(OCRUtils.getExtendedRect(rawText.getRect(), croppedPhoto), precision);
                if (tempResultList != null) {
                    for (int j = 0; j < tempResultList.size(); j++) {
                        resultTexts.add(tempResultList.get(j));
                        Log.d("OcrAnalyzer", "Found target string in: " + rawText.getDetection() + "\nwith value: " + tempResultList.get(j).getDetection());
                    }
                }
            }
        }
        resultTexts = OCRUtils.orderRawTexts(resultTexts);
        if (resultTexts.size() ==0) {
            Log.d("OcrAnalyzer", "Nothing found ");
        }
        else {
            Log.d("OcrAnalyzer", "Final list: ");
            for (RawBlock.RawText rawText : resultTexts) {
                Log.d("OcrAnalyzer", "Value: " + rawText.getDetection());
            }
        }
        return resultTexts;
    }

    /**
     * Starts and closes a TextRecognizer on chosen photo
     * @param photo photo to analyze
     * @param service main service for the detector to run
     * @return list of all blocks found
     */
    static List<TextBlock> initAnalysis(Bitmap photo, Service service) {
        SparseArray<TextBlock> origTextBlocks;
        TextRecognizer textRecognizer = new TextRecognizer.Builder(service).build();
        try {
            Frame frame = new Frame.Builder().setBitmap(photo).build();
            origTextBlocks = textRecognizer.detect(frame);
        }
        finally {
            textRecognizer.release();
        }
        List<TextBlock> orderedTextBlocks = new ArrayList<>();
        for (int i = 0; i < origTextBlocks.size(); i++) {
            orderedTextBlocks.add(origTextBlocks.valueAt(i));
        }
        return orderedTextBlocks;
    }
}
