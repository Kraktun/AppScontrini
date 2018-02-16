package com.ing.software.ocr;

import java.util.Locale;

/**
 * @author Michelon
 * @author Zaglia
 * Object passed to the manager to avoid performing unnecessary operations
 * and consequently reduce time (time depends primarily on image scale)
 */
public class OcrOptions {

    //NB: the flags inside these enums are ordered in a certain way to make use of ordinal().
    // Do not reorder.
    public enum Resolution {

        /**
         * Use original image
         */
        NORMAL,

        /**
         * Downscale image to 1/2
         */
        HALF,

        /**
         * Downscale image to 1/3
         */
        THIRD,
    }

    public enum DateSearch {

        /**
         * Skip search
         */
        SKIP,

        /**
         * Use fast detected texts
         */
        NORMAL,
    }

    public enum TotalSearch {

        /**
         * Skip search
         */
        SKIP,

        /**
         * Use fast detected texts
         */
        NORMAL,

        /**
         * Redo ocr on amount strip
         */
        DEEP,

        /**
         * Redo search if first scan did not find any valid price (up to 3 searches).
         */
        EXTENDED_SEARCH,
    }

    public enum ProductsSearch {

        /**
         * Skip search
         */
        SKIP,

        /**
         * Use fast detected texts
         */
        NORMAL,

        /**
         * Redo ocr on prices strip
         */
        DEEP,
    }

    public enum Orientation {

        /**
         * Scan image as passed
         */
        NORMAL,

        /**
         * Rescan image upside down if nothing was found
         */
        ALLOW_UPSIDE_DOWN,

        /**
         * Force scan on upside down image
         */
        FORCE_UPSIDE_DOWN,

    }

    public enum PriceEditing {

        /**
         * Do not try to edit first detected price
         */
        SKIP,

        /**
         * Allow edit of price, if a more suitable price was decoded from structure
         */
        ALLOW_STRICT,

        /**
         * Allow edit of price and try to find a price if no text matched an amount string
         */
        ALLOW_LOOSE,
    }

    private static final Resolution DEFAULT_RESOLUTION = Resolution.HALF;
    private static final TotalSearch DEFAULT_TOTAL_SEARCH = TotalSearch.DEEP;
    private static final DateSearch DEFAULT_DATE_SEARCH = DateSearch.NORMAL;
    private static final ProductsSearch DEFAULT_PRODUCTS_SEARCH = ProductsSearch.DEEP;
    private static final Orientation DEFAULT_ORIENTATION = Orientation.NORMAL;
    private static final Locale DEFAULT_COUNTRY = Locale.ITALY;
    private static final PriceEditing DEFAULT_EDIT = PriceEditing.SKIP;


    Resolution resolution = Resolution.NORMAL;
    TotalSearch totalSearch = TotalSearch.SKIP;
    DateSearch dateSearch = DateSearch.SKIP;
    ProductsSearch productsSearch = ProductsSearch.SKIP;
    Orientation orientation = Orientation.NORMAL;
    PriceEditing priceEditing = DEFAULT_EDIT;
    Locale suggestedCountry = null;

    /**
     * Return default Options
     * @return default options
     */
    public static OcrOptions getDefault() {
        return new OcrOptions()
                .total(DEFAULT_TOTAL_SEARCH)
                .resolution(DEFAULT_RESOLUTION)
                .date(DEFAULT_DATE_SEARCH)
                .products(DEFAULT_PRODUCTS_SEARCH)
                .orientation(DEFAULT_ORIENTATION)
                .suggestedCountry(DEFAULT_COUNTRY);
    }

    /**
     * Set resolution level
     * @param level resolution level
     * @return OcrOptions instance
     */
    public OcrOptions resolution(Resolution level) {
        resolution = level;
        return this;
    }

    /**
     * Set total search criteria
     * @param criteria how to search total
     * @return OcrOptions instance
     */
    public OcrOptions total(TotalSearch criteria) {
        totalSearch = criteria;
        return this;
    }

    /**
     * Set date search criteria
     * @param criteria how to search date
     * @return OcrOptions instance
     */
    public OcrOptions date(DateSearch criteria) {
        dateSearch = criteria;
        return this;
    }

    /**
     * Set products search criteria
     * @param criteria how to search products
     * @return OcrOptions instance
     */
    public OcrOptions products(ProductsSearch criteria) {
        productsSearch = criteria;
        return this;
    }

    /**
     * Set orientation criteria
     * @param criteria how to set orientation
     * @return OcrOptions instance
     */
    public OcrOptions orientation(Orientation criteria) {
        orientation = criteria;
        return this;
    }

    /**
     * Set suggested country, used to resolve ambiguities.
     * Can be extracted from current location or location history.
     * @param country ISO locale country
     * @return OcrOptions instance
     */
    public OcrOptions suggestedCountry(Locale country) {
        suggestedCountry = country;
        return this;
    }

    /**
     * Set permission to edit total
     * @param edit permission to edit total
     * @return OcrOptions instance
     */
    public OcrOptions priceEditing(PriceEditing edit) {
        priceEditing = edit;
        return this;
    }

    /**
     * Translate resolution enum value into a numeric multiplier
     * @return multiplier
     */
    double getResolutionMultiplier() { return 1. / (resolution.ordinal() + 1); }
}
