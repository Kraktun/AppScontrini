package com.ing.software.ticketapp.OCR;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.ing.software.ticketapp.OCR.OcrObjects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ing.software.ticketapp.OCR.OcrUtils.log;
import static com.ing.software.ticketapp.OCR.OcrVars.*;

/**
 * Class containing different methods to analyze a picture
 * @author Michelon
 * @author Zaglia
 */
class OcrAnalyzer {

    private TextRecognizer ocrEngine = null;
    private OnOcrResultReadyListener ocrResultCb = null;
    private Context context;
    private RawImage mainImage;
    private final int targetPrecision = 100; //Should be passed with image, or calculated with
        //resolution of source image


    /**
     * @author Michelon
     * @author Zaglia
     * Initialize the component.
     * If this call returns -1, check if the device has enough free disk space.
     * If so, try to call this method again.
     * When this method returned 0, it will be possible to call getOcrResult.
     * @param ctx Android context.
     * @return 0 if successful, negative otherwise.
     */
    int initialize(Context ctx) {
        context = ctx;
        ocrEngine = new TextRecognizer.Builder(ctx).build();
        ocrEngine.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {
                ocrEngine.release();
            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                //check if getOcrResult has been called to assign ocrResultCb.
                if (ocrResultCb != null) {
                    SparseArray<TextBlock> tempArray = detections.getDetectedItems();
                    List<RawBlock> rawBlocks = orderBlocks(mainImage, tempArray);
                    List<RawStringResult> valuedTexts = searchContinuousString(rawBlocks, AMOUNT_STRING);
                    valuedTexts = searchContinuousStringExtended(rawBlocks, valuedTexts, targetPrecision);
                    List<RawGridResult> dateList = getDateList(rawBlocks);
                    OcrResult newOcrResult = new OcrResult(valuedTexts, dateList);
                    ocrResultCb.onOcrResultReady(newOcrResult);
                }
            }
        });

        return ocrEngine.isOperational() ? 0 : -1;
        //failure causes: GSM package is not yet downloaded due to lack of time or lack of space.
    }

    /**
     * @author Michelon
     * @author Zaglia
     * Get an OcrResult from a Bitmap
     * @param frame Bitmap from which to extract an OcrResult. Not null.
     * @param resultCb Callback to get an OcrResult. Not null.
     * NOTE: MUST BE HANDLED WHEN frame (from getCroppedPhoto) IS NULL
     */
    void getOcrResult(@NonNull Bitmap frame, OnOcrResultReadyListener resultCb){
        ocrResultCb = resultCb;
        //must be used somewhere else. Now waiting for processing with opencv
        //frame = getCroppedPhoto(frame, context);
        if (frame == null)
            log(1,"getOcrResult", "cropped image is null ");
        mainImage = new RawImage(frame);
        ocrEngine.receiveFrame(new Frame.Builder().setBitmap(frame).build());
    }

    /**
     * @author Michelon
     * Orders TextBlock decoded by detector in a list of RawBlocks
     * Order is from top to bottom, from left to right
     * @param photo RawImage of the photo
     * @param origTextBlocks detected blocks
     * @return list of ordered RawBlocks
     */
    private static List<RawBlock> orderBlocks(@NonNull RawImage photo, @NonNull SparseArray<TextBlock> origTextBlocks) {
        log(2,"OcrAnalyzer.analyzeST:" , "Preferred grid is: " + photo.getGrid());
        List<TextBlock> newOrderedTextBlocks = new ArrayList<>();
        for (int i = 0; i < origTextBlocks.size(); i++) {
            newOrderedTextBlocks.add(origTextBlocks.valueAt(i));
        }
        newOrderedTextBlocks = OcrUtils.orderBlocks(newOrderedTextBlocks);
        log(2,"OcrAnalyzer.analyzeST:" , "New Blocks ordered");
        List<RawBlock> rawBlocks = new ArrayList<>();
        for (TextBlock textBlock : newOrderedTextBlocks) {
            rawBlocks.add(new RawBlock(textBlock, photo));
        }
        return rawBlocks;
    }

    /**
     * @author Michelon
     * Find first (RawTexts in RawBlocks are ordered from top to bottom, left to right)
     * occurrence (exact match) of chosen string in a list of RawBlocks
     * @param rawBlocks list of RawBlocks
     * @param testString string to find
     * @return First RawText in first RawBlock with target string
     */
    private static RawText searchFirstExactString(@NonNull List<RawBlock> rawBlocks, @Size(min = 1) String testString) {
        RawText targetText = null;
        for (RawBlock rawBlock : rawBlocks) {
            targetText = rawBlock.findFirstExact(testString);
            if (targetText != null)
                break;
        }
        if (targetText != null) {
            log(3,"OcrAnalyzer.analyzeBFS", "Found first target string: "+ testString + " \nat: " + targetText.getDetection());
            log(3,"OcrAnalyzer.analyzeBFS", "Target text is at (left, top, right, bottom): "+ targetText.getRect().left + "; "
                    + targetText.getRect().top + "; " + targetText.getRect().right + "; "+ targetText.getRect().bottom + ".");
        }
        return targetText;
    }

    /**
     * @author Michelon
     * Search for all occurrences of target string in detected (and ordered) RawBlocks according to OcrVars.MAX_STRING_DISTANCE
     * @param rawBlocks list of RawBlocks
     * @param testString string to find.
     * @return list of RawStringResult containing only source RawText where string is present. Note: this list is not ordered.
     */
    private static List<RawStringResult> searchContinuousString(@NonNull List<RawBlock> rawBlocks, @Size(min = 1) String testString) {
        List<RawStringResult> targetTextList = new ArrayList<>();
        for (RawBlock rawBlock : rawBlocks) {
            List<RawStringResult> tempTextList = rawBlock.findContinuous(testString);
            if (tempTextList != null) {
                targetTextList.addAll(tempTextList);
            }
        }
        if (targetTextList.size() >0 && IS_DEBUG_ENABLED) {
            for (RawStringResult stringText : targetTextList) {
                RawText text = stringText.getSourceText();
                log(3,"OcrAnalyzer", "Found target string: " + testString + " \nat: " + text.getDetection()
                        + " with distance: " + stringText.getDistanceFromTarget());
                log(3,"OcrAnalyzer", "Target text is at (left, top, right, bottom): " + text.getRect().left
                        + "; " + text.getRect().top + "; " + text.getRect().right + "; " + text.getRect().bottom + ".");
            }
        }
        return targetTextList;
    }

    /**
     * @author Michelon
     * From a list of RawTexts, retrieves also RawTexts with similar distance from top and bottom of the photo.
     * 'Similar' is defined by precision. See RawBlock.findByPosition() for details.
     * @param rawBlocks list of RawBlocks from original photo
     * @param targetStringList list of target RawTexts
     * @param precision precision to extend rect. See RawBlock.RawText.extendRect()
     * @return list of RawStringResults. Adds to the @param targetStringList objects the detected RawTexts
     * in proximity of source RawTexts. Note: this list is not ordered.
     */
    /*La ricerca proceed così:
    - prendi un blocco
    - cerca se il rettangolo esteso di un source di stringresult contiene uno o più text del blocco
    - se si salva la lista e aggiungila allo stringresult
    - ripeti dal punto 2 finché non finisci gli stringresult
    - ripeti dal punto 1 finché non finisci i block
    */
    private static List<RawStringResult> searchContinuousStringExtended(@NonNull List<RawBlock> rawBlocks, @NonNull List<RawStringResult> targetStringList, int precision) {
        List<RawStringResult> results = targetStringList;
        log(2,"OcrAnalyzer.SCSE", "StringResult list size is: " + results.size());
        for (RawBlock rawBlock : rawBlocks) {
            for (RawStringResult singleResult : results) {
                RawText rawText = singleResult.getSourceText();
                log(3,"OcrAnalyzer.SCSE", "Extending rect: " + rawText.getDetection());
                List<RawText> tempResultList = rawBlock.findByPosition(OcrUtils.getExtendedRect(rawText.getRect(), rawText.getRawImage()), precision);
                if (tempResultList != null) {
                    singleResult.addDetectedTexts(tempResultList);
                    log(3,"OcrAnalyzer", "Found target string in extended: " + rawText.getDetection() + "\nin " + tempResultList.size() + " blocks.");
                }
                else
                    log(3,"OcrAnalyzer.SCSE", "Nothing found"); //Nothing in this block
            }
        }
        if (results.size() == 0) {
            log(2,"OcrAnalyzer", "Nothing found ");
        }
        else if (IS_DEBUG_ENABLED){
            log(2,"OcrAnalyzer", "Final list: " + results.size());
            for (RawStringResult rawStringResult : results) {
                List<RawText> textList = rawStringResult.getDetectedTexts();
                if (textList == null)
                    log(2,"OcrAnalyzer.SCSE", "Value not found.");
                else {
                    for (RawText rawText : textList) {
                        log(2,"OcrAnalyzer.SCSE", "Value: " + rawText.getDetection());
                        log(2,"OcrAnalyzer.SCSE", "Source: " + rawStringResult.getSourceText().getDetection());
                    }
                }
            }
        }
        return results;
    }

    /**
     * @author Michelon
     * Merges Lists with RawTexts + probability to find date from all blocks.
     * And orders it.
     * @param rawBlocks blocks from which retrieve lists
     * @return List of RawGridResults containing RawTexts + probability
     */
    private List<RawGridResult> getDateList(@NonNull List<RawBlock> rawBlocks) {
        List<RawGridResult> fullList = new ArrayList<>();
        for (RawBlock rawBlock : rawBlocks) {
            fullList.addAll(rawBlock.getDateList());
        }
        log(2,"FINAL_LIST_SIZE_IS", " " + fullList.size());
        Collections.sort(fullList);
        return fullList;
    }

    /**
     * @author Michelon
     * Performs a quick detection on chosen photo and returns blocks detected
     * @param photo photo to analyze
     * @param context context to run analyzer
     * @return list of all blocks found (ordered from top to bottom, left to right)
     */
    private static List<TextBlock> quickAnalysis(@NonNull Bitmap photo, Context context) {
        SparseArray<TextBlock> origTextBlocks;
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
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

    /**
     * NOTE: This is a temporary method.
     * @author Michelon
     * Crops photo using the smallest rect that contains all TextBlock in source photo
     * @param photo source photo
     * @param context context to run first analyzer
     * @return smallest cropped photo containing all TextBlocks
     */
    public static Bitmap getCroppedPhoto(@NonNull Bitmap photo, Context context) {
        List<TextBlock> orderedTextBlocks = quickAnalysis(photo, context);
        log(2,"OcrAnalyzer.analyze:" , "Blocks detected");
        orderedTextBlocks = OcrUtils.orderBlocks(orderedTextBlocks);
        log(2,"OcrAnalyzer.analyze:" , "Blocks ordered");
        int[] borders = OcrUtils.getRectBorders(orderedTextBlocks, new RawImage(photo));
        int left = borders[0];
        int right = borders[2];
        int top = borders[1];
        int bottom = borders[3];
        return OcrUtils.cropImage(photo, left, top, right, bottom);
    }
}
