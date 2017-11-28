package com.ing.software.ticketapp.OCR;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Size;
import android.util.Log;

import com.google.android.gms.vision.text.TextBlock;
import com.ing.software.ticketapp.OCR.OcrObjects.RawImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.ing.software.ticketapp.OCR.OcrVars.LOG_LEVEL;

/**
 * Util class to manage rects, blocks, images
 *
 */

public class OcrUtils {

    /**
     * @author Michelon
     * Crop image (values start from top left)
     * @param photo original photo not null. Not null.
     * @param startX x coordinate of top left point, int >= 0
     * @param startY y coordinate of top left point, int >= 0
     * @param endX x coordinate of bottom right point, int >= 0
     * @param endY y coordinate of bottom right point, int >= 0
     * @return cropped image, null if invalid coordinates
     */
    static Bitmap cropImage(@NonNull Bitmap photo, @IntRange(from = 0) int startX, @IntRange(from = 0) int startY, @IntRange(from = 0) int endX, @IntRange(from = 0) int endY) {
        log(2,"OcrUtils.cropImage","Received crop: left " + startX + " top: " + startY + " right: " + endX + " bottom: " + endY);
        if (endX < startX || endY < startY)
            return null;
        int width = Math.abs(endX - startX);
        int height = Math.abs(endY - startY);
        return Bitmap.createBitmap(photo, startX, startY, width, height);
    }

    /**
     * @author Michelon
     * Get rect containing all blocks detected (Temporary method)
     * Note: counting starts from left and from top
     * @param orderedTextBlocks blocks detected. Not null.
     * @param photo original photo. Not null.
     * @return array of int where int[0] = left border, int[1] = top border, int[2] = right border, int[3] = bottom border
     */
    static int[] getRectBorders(@NonNull List<TextBlock> orderedTextBlocks, @NonNull RawImage photo) {
        int numberOfBorders = 4; //it's a rect
        int[] borders = new int[numberOfBorders];
        //Extreme borders for chosen photo (will be overwritten in foreach)
        int left = photo.getWidth();
        int right = 0;
        int top = photo.getHeight();
        int bottom = 0;
        for (TextBlock textBlock : orderedTextBlocks) {
            RectF rectF = new RectF(textBlock.getBoundingBox());
            if (rectF.left<left)
                left = Math.round(rectF.left);
            if (rectF.right>right)
                right = Math.round(rectF.right);
            if (rectF.bottom>bottom)
                bottom = Math.round(rectF.bottom);
            if (rectF.top<top)
                top = Math.round(rectF.top);
            log(4,"OcrUtils.getRectBorder","Value: " + textBlock.getValue());
            log(4,"OcrUtils.getRectBorder","Temp rect: (left, top, right, bottom): " + rectF.left + "; " + rectF.top + "; " + rectF.right + "; " + rectF.bottom);
        }
        borders[0] = left;
        borders[1] = top;
        borders[2] = right;
        borders[3] = bottom;
        log(2,"OcrUtils.getRectBorder","New rect: (left, top, right, bottom): " + left + "; " + top + "; " + right + "; " + bottom);
        return borders;
    }

    /**
     * @author Michelon
     * Get preferred grid according to height/width ratio
     * @param photo original photo. Not null.
     * @return preferred ratio defined in ProbGrid, -1 if something went wrong
     */
     public static String getPreferredGrid(@NonNull Bitmap photo) {
        double width = photo.getWidth();
        double heigth = photo.getHeight();
        String preferredRatio = "-1";
        if (width <= 0 || heigth <= 0)
            return preferredRatio;
        double ratio = heigth/width;
        List<Double> availableRatios = new ArrayList<>(ProbGrid.gridMap.keySet());
        double diff = Double.MAX_VALUE;
        for(Double testRatio : availableRatios) {
            if (Math.abs(testRatio-ratio)<diff) {
                diff = Math.abs(testRatio - ratio);
                preferredRatio = ProbGrid.gridMap.get(testRatio);
            }
        }
        log(2,"OcrUtils.getPrefGrid","Ratio is: " + ratio + " Grid is: " + preferredRatio + " Diff is: " + diff);
        return preferredRatio;
    }

    /**
     * @author Michelon
     * Order a list of TextBlock from top to bottom, left to right
     * @param textBlocks original list. Not null.
     * @return ordered list
     */
    static List<TextBlock> orderBlocks(@NonNull List<TextBlock> textBlocks) {
        Collections.sort(textBlocks, new Comparator<TextBlock>() {
            @Override
            public int compare(TextBlock block1, TextBlock block2) {
                int diffTops = block1.getBoundingBox().top - block2.getBoundingBox().top;
                int diffLefts = block1.getBoundingBox().left - block2.getBoundingBox().left;
                int diffBottoms = block1.getBoundingBox().bottom - block2.getBoundingBox().bottom;
                int diffRights = block1.getBoundingBox().right - block2.getBoundingBox().right;
                if (diffTops != 0) {
                    return diffTops;
                }
                else if (diffLefts != 0)
                    return diffLefts;
                else if (diffBottoms != 0)
                    return diffBottoms;
                else
                    return diffRights;
            }
        });
        return textBlocks;
    }

    /**
     * @author Michelon
     * Extends the width of a rect to the max allowed for chosen photo
     * @param rect source rect. Not null.
     * @param photo source photo (to get max width). Not null.
     * @return rect with max width
     */
    static RectF getExtendedRect(@NonNull RectF rect, @NonNull RawImage photo) {
        float top = rect.top;
        float bottom = rect.bottom;
        float left = 0;
        float right = photo.getWidth();
        RectF rectF = new RectF(left, top, right, bottom);
        log(2,"OcrUtils.getExtendRect","Extended rect: left " + rectF.left + " top: "
                + rectF.top + " right: " + rectF.right + " bottom: " + rectF.bottom);
        return rectF;
    }

    /**
     * @author Michelon
     * Logs messages only if debug is enabled
     * @param level int >= 0, only messages with level <= OcrVars.LOG_LEVEL are logged
     * @param tag tag of the message to log, must be less than 24 chars long
     * @param message message to log
     */
    public static void log(@IntRange(from = 0) int level, @Size(max = 23) String tag, String message) {
        if (OcrVars.IS_DEBUG_ENABLED && level <= LOG_LEVEL)
            Log.d(tag, message);
    }


    /**
     * Returns the maximum between |i| e |j|
     *
     * @param i The first integer not null to be compared
     * @param j The second integer not null to be compared
     * @return maximum number
     */
    private static int maxLengthStrings(int i, int j)
    {
        return (i>j ? i : j);
    }

    /**
     * Returns the minimum between |i|, |j| e |k|
     *
     * @param i The first integer not null to be compared
     * @param j The second integer not null to be compared
     * @param k The third integer not null to be compared
     * @return minimum number
     */
    private static int minLengthStrings(int i, int j, int k)
    {
        int result = i;
        if (j < result) result = j;
        if (k < result) result = k;
        return result;
    }

    /**
     * Returns the distance of Levenshtein between two strings | S | e | T |.
     * The distance is an integer between 0 and the maximum length of the two strings.
     * If only one string is null then return -1
     *
     * @param S The first string to be compared
     * @param T The second string to be compared
     * @return distance between two strings
     */
    private static int levDistance(String S, String T )
    {
        if(S == null || T == null)
            return -1;

        int i, j;
        final int n = S.length(), m = T.length();
        int L[][] = new int[n+1][m+1];
        for ( i=0; i<n+1; i++ ) {
            for ( j=0; j<m+1; j++ ) {
                if ( i==0 || j==0 ) {
                    L[i][j] = maxLengthStrings(i, j);
                } else {
                    L[i][j] = minLengthStrings(L[i-1][j] + 1, L[i][j-1] + 1,
                            L[i-1][j-1] + (S.charAt(i-1) != T.charAt(j-1) ? 1 : 0) );
                }
            }
        }

        return L[n][m];
    }

    /**
     * Check if there is a substring in the text
     * The text is subdivided into tokens and each token is checked
     * If only one string is null then return -1
     * If the token length is less than 2 then returns -1 -1
     *
     * @param text The text to be compared
     * @param substring The second string to be compared
     * @return the slightest difference between strings and text
     */
    public static int findSubstring(String text, String substring)
    {
        if(text.length() == 0)
            return -1;

        if(text == null || substring == null)
            return -1;

        int minDistance = substring.length();

        //Splits the string into tokens
        String[] pack = text.split("\\s");

        for (String p: pack){
                //Convert string to uppercase
                int distanceNow = levDistance(p.toUpperCase(), substring.toUpperCase());
                if (distanceNow < minDistance)
                    minDistance = distanceNow;

        }

        return minDistance;

    }




    /**
     * Accept a text and check if there is a combination of date format.
     * Controllo per tutte le combinazioni simili a
     * xx/xx/xxxx o xx/xx/xxxx o xxxx/xx/xx
     * xx-xx-xxxx o xx-xx-xxxx o xxxx-xx-xx
     * xx.xx.xxxx o xx.xx.xxxx xxxx.xx.xx
     *
     * @param text The text to find the date format
     * @return the absolute value of the minimum distance found between all combinations,
     * if the distance is >= 10 or the inserted text is empty returns -1
     */
    private static int findDate(String text) {
        if (text.length() == 0)
            return -1;

        //Splits the string into tokens
        String[] pack = text.split("\\s");

        String[] formatDate = {"xx/xx/xxxx", "xx/xx/xxxx", "xxxx/xx/xx","xx-xx-xxxx", "xx-xx-xxxx", "xxxx-xx-xx", "xx.xx.xxxx", "xx.xx.xxxx", "xxxx.xx.xx"};

        //Maximum number of characters in the date format
        int minDistance = 10;
        //Th eminimum of number combinations of date format without symbols like '/' or '.' or '-'
        int minCharaterDate = 8;

        for (String p : pack) {
            for (String d : formatDate) {
                //Convert string to uppercase
                int distanceNow = levDistance(p.toUpperCase(), d.toUpperCase());
                if (distanceNow < minDistance)
                    minDistance = distanceNow;
            }
        }

        if(minDistance==10)
            return -1;
        else
            //Returns the absolute value of the distance by subtracting the minimum character
            return Math.abs(minCharaterDate-minDistance);

    }


    /**
     * It takes a text and returns the date if a similarity is found with a date format
     *
     * @param text The text to find the date
     * @return date or null if the date is not there
     */
    private static String getDate(String text) {
        if (text.length() == 0)
            return null;

        //Splits the string into tokens
        String[] pack = text.split("\\s");

        String[] formatDate = {"xx/xx/xxxx", "xx/xx/xxxx", "xxxx/xx/xx","xx-xx-xxxx", "xx-xx-xxxx", "xxxx-xx-xx", "xx.xx.xxxx", "xx.xx.xxxx", "xxxx.xx.xx"};

        //Maximum number of characters in the date format
        int minDistance = 10;
        String dataSearch = null;

        for (String p : pack) {
            for (String d : formatDate) {
                //Convert string to uppercase
                int distanceNow = levDistance(p.toUpperCase(), d.toUpperCase());
                if (distanceNow < minDistance)
                {
                    minDistance = distanceNow;
                    dataSearch = p.toUpperCase();
                }

            }
        }


        return dataSearch;
    }
}
