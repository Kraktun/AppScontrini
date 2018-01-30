package com.ing.software.ocr;

import android.graphics.RectF;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import com.ing.software.ocr.OcrObjects.TempText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.ing.software.ocr.OcrVars.*;

/**
 * Manages a receipt trying to find its elements (products, blocks etc)
 * WIP
 */

public class OcrSchemer {

    /**
     * Organize a list of rawTexts adding tags according to their position.
     * @param textList list of rawTexts to organize. Not null
     */
    static void prepareScheme(@NonNull List<TempText> textList) {
        for (TempText rawText : textList) {
            switch (getPosition(rawText)) {
                case 0: rawText.addTag(LEFT_TAG);
                    OcrUtils.log(4, "prepareScheme", "Text: "+rawText.text() + " is left");
                    break;
                case 1: rawText.addTag(CENTER_TAG);
                    OcrUtils.log(4, "prepareScheme", "Text: "+rawText.text() + " is center");
                    break;
                case 2: rawText.addTag(RIGHT_TAG);
                    OcrUtils.log(4, "prepareScheme", "Text: "+rawText.text() + " is right");
                    break;
                default: OcrUtils.log(1, "prepareScheme", "Could not find position for text: " + rawText.text());
            }
        }
        int targetHeight = getBestCentralArea(textList);
        centralTagger(textList, targetHeight);
        missTagger(textList);
        Collections.sort(textList);
        int endingIntroduction = densitySnake(textList, INTRODUCTION_TAG);
        if (endingIntroduction != -1)
            intervalTagger(textList, INTRODUCTION_TAG, 0, endingIntroduction);
        //else
        //   endingIntroduction = 0;
        List<TempText> reversedTextList = new ArrayList<>(textList);
        Collections.reverse(reversedTextList);
        int startingConclusion = densitySnake(reversedTextList, CONCLUSION_TAG);
        if (startingConclusion != -1) {
            startingConclusion = textList.size() - startingConclusion - 1;
            intervalTagger(textList, CONCLUSION_TAG, startingConclusion, textList.size() - 1);
        } else
            startingConclusion = textList.size(); // -1?
        intervalTagger(textList, PRODUCTS_TAG, PRODUCTS_TAG, PRICES_TAG, endingIntroduction + 1, startingConclusion -1);
        OcrManager.mainImage.textFitter(); //save configuration from prepareScheme in rawimage
    }

    /**
     * Find column where center of rawText is, dividing the maximized rect in a defined number of parts parts
     * @param text target text. Not null.
     * @return 0,1,2 according to position
     */
    static private int getPosition(@NonNull TempText text) {
        //int width = text.getRawImage().getWidth();
        RectF maxRect = OcrManager.mainImage.getExtendedRect();
        float width = maxRect.width();
        float position = (text.box().centerX() - maxRect.left)*13/width;
        if (0<=position && position<=4)
            return 0;
        else if (position<=9)
            return 1;
        else
            return 2;
        //return Math.round(text.getBoundingBox().centerX())*3/width;
    }

    /**
     * @param text source text. Not null.
     * @param targetHeight target height
     * @return True if text is above target height
     */
    private static boolean isHalfUp(@NonNull TempText text, int targetHeight) {
        //int height = text.getRawImage().getHeight();
        //return text.getBoundingBox().centerY() < height/2;
        return text.box().centerY() < targetHeight;
    }

    /**
     * Cerca l'area dello scontrino in cui hai il maggior numero di text left\right
     * per decidere dove far finire introduction e dove iniziare conclusion
     * @param texts
     * @return
     */
    private static int getBestCentralArea(List<TempText> texts) {
        Collections.sort(texts);
        int areaDivider = 4;
        if (texts.size() == 0)
            return -1;
        RectF maximRect = OcrManager.mainImage.getExtendedRect();
        float targetHeight = OcrManager.mainImage.getExtendedRect().height()/areaDivider;
        SortedMap<Double, Integer> map = new TreeMap<>();
        for (int i = 0; i < texts.size(); ++i) {
            TempText rawText = texts.get(i);
            RectF areaRect = new RectF(maximRect.left, rawText.box().top, maximRect.right, rawText.box().top + targetHeight);
            if (areaRect.bottom > maximRect.bottom)
                break;
            double leftRightArea = 0;
            double targetArea = 0;
            for (TempText text : texts) {
                if (areaRect.contains(text.box())) {
                    if (text.getTags().contains(LEFT_TAG) || text.getTags().contains(RIGHT_TAG)) {
                        leftRightArea += getRectArea(text.box());
                    }
                    targetArea += getRectArea(text.box());
                }
            }
            if (targetArea == 0) //should never happen
                targetArea = 1;
            OcrUtils.log(5, "getBestCentralArea", "Partial density for: " + rawText.text() + " is: " + leftRightArea / targetArea);
            map.put(leftRightArea / targetArea, i);
        }
        TempText chosenRect = texts.get(map.get(map.lastKey()));
        OcrUtils.log(3, "getBestCentralArea", "Best center is at: " + (chosenRect.box().centerY())
            + " of rect " + chosenRect.text() + " and density: " + map.lastKey());
        return (int)chosenRect.box().centerY();
    }

    /**
     * Organize texts, if they are on the same level of a introduction\conclusion text, tag them accordingly
     * else consider them products or prices. (todo: simplify)
     * @param texts ordered list of RawTexts. Not null.
     * @param source source RawText. Not null.
     */
    private static void waveTagger(@NonNull List<TempText> texts, @NonNull TempText source) {
        int extendHeight = 20;
        RectF extendedRect = OcrUtils.extendRect(source.box(), extendHeight, -OcrManager.mainImage.getWidth());
        for (TempText text : texts) {
            if (!text.getTags().contains(CENTER_TAG)) { //here we are excluding source
                if (extendedRect.contains(text.box())) {
                    if (source.getTags().contains(INTRODUCTION_TAG)) {
                        text.addTag(INTRODUCTION_TAG);
                        OcrUtils.log(4, "waveTagger", "Text: " + text.text() + " is introduction");
                    } else {
                        text.addTag(CONCLUSION_TAG);
                        OcrUtils.log(4, "waveTagger", "Text: " + text.text() + " is conclusion");
                    }
                }
            }
        }
    }

    /**
     * Tags central texts according to position
     * @param texts ordered list of RawTexts. Not null.
     * @param targetHeight
     */
    private static void centralTagger(@NonNull List<TempText> texts, int targetHeight) {
        for (TempText rawText : texts) {
            if (rawText.getTags().contains(CENTER_TAG)) { //here we are excluding source
                if (isHalfUp(rawText, targetHeight)) {
                    rawText.addTag(INTRODUCTION_TAG);
                    OcrUtils.log(4, "centralTagger", "Text: " + rawText.text() + " is center-introduction");
                } else {
                    rawText.addTag(CONCLUSION_TAG);
                    OcrUtils.log(4, "centralTagger", "Text: "+rawText.text() + " is center-conclusion");
                }
                waveTagger(texts, rawText);
            }
        }
    }

    /**
     * Tag rawTexts without a introduction\conclusion tag, as products\prices
     * @param texts list of rawTexts. Not null.
     */
    private static void missTagger(@NonNull List<TempText> texts) {
        for (TempText text : texts) {
            if (!text.getTags().contains(INTRODUCTION_TAG) && !text.getTags().contains(CONCLUSION_TAG)) {
                if (text.getTags().contains(LEFT_TAG)) {
                    text.addTag(PRODUCTS_TAG);
                    OcrUtils.log(4, "missTagger", "Text: " + text.text() + " is possible product");
                } else {
                    text.addTag(PRICES_TAG);
                    OcrUtils.log(4, "missTagger", "Text: " + text.text() + " is possible price");
                }
            }
        }
    }

    /**
     * Get index of rect at which you want to stop tag 'block'
     * Method comments are an example of introduction tag
     * @param textList list of rawTexts. Not null.
     * @param tag tag for current block. Not null.
     * @return -1 if list is empty or there is no tag block (too low density), else index of last block of tag
     */
    private static int densitySnake(@NonNull List<TempText> textList, @NonNull String tag) {
        if (textList.size() == 0)
            return -1;
        double targetArea = 0;
        double totalArea = 0;
        SortedMap<Double, Integer> map = new TreeMap<>(); //Key is the density, Value is the position, the bigger the area for same density, the better it is
        for (int i = 0; i < textList.size(); ++i) {
            TempText text = textList.get(i);
            if (text.getTags().contains(tag)) {
                targetArea += getRectArea(text.box());
            }
            totalArea += getRectArea(text.box());
            OcrUtils.log(5, "densitySnake", "Text: " + text.text() + "\ncurrent density is: "
                + targetArea/totalArea + "\nand tag: " + tag);
            map.put(targetArea/totalArea, i); // if we have same density but bigger area, let's choose biggest area
        }
        //here We have a list of ordered density and area, to choose the best we define a variable (needs to be tuned)
        // N = {(area of 1 rect)*[(n° of rect)-(n° of rect/15)]/(total area)}
        int limitText = Math.round(textList.size()/15);
        if (limitText == 0)
            limitText = 1;
        double limit = ((totalArea/textList.size())*(textList.size() - limitText)/totalArea);
        OcrUtils.log(5, "densitySnake", "Limit density is: " + limit);
        double targetDensity = -1;
        for (Double currentDensity : map.keySet()) {
            OcrUtils.log(5, "densitySnake", "Possible density is: " + currentDensity);
            if (currentDensity > limit) {
                targetDensity = currentDensity;
                break;
            }
        }
        //Now, we may have included some wrong texts (suppose we have all blocks introduction, followed by all products,
        // here we include also the first few products), so we better reduce area to the first introduction rect
        if (targetDensity == -1)
            return -1; //No introduction block found, or density too low
        int position = map.get(targetDensity);
        for (int i = position; i > 0; --i) {
            if (textList.get(i).getTags().contains(tag)) {
                position = i;
                break;
            }
        }
        OcrUtils.log(2, "densitySnake", "Final target text is: " + position
            + "\nWith text: " + textList.get(position).text() + "\nand tag: " + tag);
        //Now position is the value of the last rect for introduction
        return position;
    }

    /**
     * Get area of a rect
     * @param rect A rect. Not null.
     * @return Area of the rect.
     */
    private static double getRectArea(@NonNull RectF rect) {
        return rect.width() * rect.height();
    }

    /**
     * Tag all texts in a interval with the same tag. Starting and ending indexes are included.
     * @param textList list of rawTexts. Not null.
     * @param tag new tag for rawTexts. Not null.
     * @param start index of first element. Int >= 0.
     * @param end index of last element. Int >= 0.
     */
    private static void intervalTagger(@NonNull List<TempText> textList, @NonNull String tag, @IntRange(from = 0) int start, @IntRange(from = 0) int end) {
        if (end < start)
            return;
        for (int i = start; i <= end; ++i) {
            TempText text = textList.get(i);
            removeOldTags(text);
            text.addTag(tag);
            //text.setTagPosition(getTextBlockPosition(text, textList.get(start).box().top, textList.get(end).box().bottom));
            OcrUtils.log(5, "intervalTagger", "Text: " + text.text() +
                    "\nis in position: " /*+ getTextBlockPosition(text, textList.get(start).box().top, textList.get(end).box().bottom)*/
                    + "\nwith tag: " + tag);
        }
    }

    /**
     * Tag all texts in a interval with the same tag. Starting and ending indexes are included.
     * @param textList list of rawTexts. Not null.
     * @param tagLeft new tag for rawTexts on left. Not null.
     * @param tagCenter new tag for rawTexts on center. Not null.
     * @param tagRight new tag for rawTexts on right. Not null.
     * @param start index of first element. Int >= 0.
     * @param end index of last element. Int >= 0.
     */
    private static void intervalTagger(@NonNull List<TempText> textList, @NonNull String tagLeft, @NonNull String tagCenter,
                                       @NonNull String tagRight, @IntRange(from = 0) int start, @IntRange(from = 0) int end) {
        if (end < start)
            return;
        for (int i = start; i <= end; ++i) {
            TempText text = textList.get(i);
            removeOldTags(text);
            if (text.getTags().contains(LEFT_TAG)) {
                text.addTag(tagLeft);
                //text.setTagPosition(getTextBlockPosition(text, textList.get(start).box().top, textList.get(end).box().bottom));
                OcrUtils.log(5, "intervalTagger", "Text: " + text.text() +
                        "\nis in position: " /*+ getTextBlockPosition(text, textList.get(start).box().top, textList.get(end).box().bottom)*/
                        + "\nwith tag: " + tagLeft);
            } else if (text.getTags().contains(RIGHT_TAG)) {
                text.addTag(tagRight);
                //text.setTagPosition(getTextBlockPosition(text, textList.get(start).box().top, textList.get(end).box().bottom));
                OcrUtils.log(5, "intervalTagger", "Text: " + text.box() +
                        "\nis in position: " /*+ getTextBlockPosition(text, textList.get(start).box().top, textList.get(end).box().bottom)*/
                        + "\nwith tag: " + tagRight);
            } else {
                text.addTag(tagCenter);
                //text.setTagPosition(getTextBlockPosition(text, textList.get(start).box().top, textList.get(end).box().bottom));
                OcrUtils.log(5, "intervalTagger", "Text: " + text.text() +
                        "\nis in position: " /*+ getTextBlockPosition(text, textList.get(start).box().top, textList.get(end).box().bottom)*/
                        + "\nwith tag: " + tagCenter);
            }
        }
    }

    /**
     * Remove all tags (excluded left-center-right) from a RawText
     * @param text source text. Not Null.
     */
    private static void removeOldTags(@NonNull TempText text) {
        text.removeTag(INTRODUCTION_TAG);
        text.removeTag(CONCLUSION_TAG);
        text.removeTag(PRODUCTS_TAG);
        text.removeTag(PRICES_TAG);
    }
}
