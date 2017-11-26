package com.ing.software.ticketapp.OCR;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import com.ing.software.ticketapp.OCR.OcrObjects.RawGridResult;
import com.ing.software.ticketapp.OCR.OcrObjects.RawStringResult;
import com.ing.software.ticketapp.OCR.OcrObjects.RawText;
import com.ing.software.ticketapp.common.Ticket;


/**
 * Class used to extract informations from raw data
 */
public class DataAnalyzer {

    private final OcrAnalyzer analyzer = new OcrAnalyzer();

    /**
     * Initialize OcrAnalyzer
     * @param context Android context
     * @return 0 if everything ok, negative number if an error occurred
     */
    public int initialize(Context context) {
        return analyzer.initialize(context);
    }

    /**
     * Get a Ticket from a Bitmap. Some fields of the new ticket can be null.
     * @param photo Bitmap. Not null.
     * @param ticketCb callback to get the ticket. Not null.
     */
    public void getTicket(@NonNull Bitmap photo, final OnTicketReadyListener ticketCb) {
        final long startTime = System.nanoTime();
        analyzer.getOcrResult(photo, new OnOcrResultReadyListener() {
            @Override
            public void onOcrResultReady(OcrResult result) {
                // for now, let's invoke the callback syncronously.
                ticketCb.onTicketReady(getTicketFromResult(result));
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                OcrUtils.log(1,"EXECUTION TIME: ", duration + " milliseconds");
            }
        });
    }

    /**
     * Coverts an OcrResult into a Ticket analyzing its data
     * @param result OcrResult to analyze. Not null.
     * @return Ticket. Some fields can be null;
     */
    private Ticket getTicketFromResult(OcrResult result) {
        Ticket ticket = new Ticket();
        List<RawGridResult> dateMap = result.getDateList();
        ticket.amount = getPossibleAmount(result.getAmountResults());
        return ticket;
    }

    /**
     * @author Michelon
     * Search through results from the research of amount string and retrieves the text with highest
     * probability to contain the amount calculated with (probability from grid - distanceFromTarget*10)
     * @param amountResults list of RawStringResult from amount search
     * @return BigDecimal containing the amount found
     */
    private BigDecimal getPossibleAmount(@NonNull List<RawStringResult> amountResults) {
        List<RawGridResult> possibleResults = new ArrayList<>();
        for (RawStringResult stringResult : amountResults) {
            RawText sourceText = stringResult.getSourceText();
            int singleCatch = sourceText.getAmountProbability() - stringResult.getDistanceFromTarget()*10;
            if (stringResult.getDetectedTexts() != null) {
                for (RawText rawText : stringResult.getDetectedTexts()) {
                    if (!rawText.equals(sourceText)) {
                        possibleResults.add(new RawGridResult(rawText, singleCatch));
                        OcrUtils.log(2,"getPossibleAmount", "Analyzing source text: " + sourceText.getDetection() +
                                " where target is: " + rawText.getDetection() + " with probability: " + sourceText.getAmountProbability() +
                                " and distance: " + stringResult.getDistanceFromTarget());
                    }
                }
            }
        }
        if (possibleResults.size() > 0) {
            Collections.sort(possibleResults);
            OcrUtils.log(2,"getPossibleAmount", "First amount is: " + possibleResults.get(0).getText().getDetection());
            BigDecimal amount;
            for (RawGridResult result : possibleResults) {
                String amountString = result.getText().getDetection();
                try {
                    amount = new BigDecimal(amountString);
                    } catch (NumberFormatException e) {
                        try {
                            amount = analyzeAmount(amountString);
                            } catch (Exception e1) {
                            amount = null;
                        }
                    } catch (Exception e2)
                    {
                    amount = null;
                    }
                if (amount != null) {
                    OcrUtils.log(2, "getPossibleAmount", "Decoded value: " + amount);
                    return amount;
                }
            }
        }
        else
            return null;
        return null;
    }

    //Prova a cercare un bigdecimal in stringhe che possono contenere lettere
    private BigDecimal analyzeAmount(String targetAmount) throws NumberFormatException {
        targetAmount = targetAmount.replaceAll(",", ".");
        StringBuilder manipulatedAmount = new StringBuilder();
        for (int i = 0; i < targetAmount.length(); ++i) {
            if (Character.isDigit(targetAmount.charAt(i)) || targetAmount.charAt(i)=='.')
                manipulatedAmount.append(targetAmount.charAt(i));
        }
        if (manipulatedAmount.toString().length() == 0)
            return null;
        return new BigDecimal(manipulatedAmount.toString());
    }
}
