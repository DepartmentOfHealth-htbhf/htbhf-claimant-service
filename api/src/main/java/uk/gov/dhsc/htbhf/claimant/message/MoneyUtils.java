package uk.gov.dhsc.htbhf.claimant.message;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Contains utility methods for dealing with monetary amounts.
 */
public class MoneyUtils {

    /**
     * Given the amount in pence, convert it to an amount in pounds correctly formatted, e.g.
     * 1298 will return a value of £12.98.
     *
     * @param amountInPence The amount in pence
     * @return The formatted amount in pounds
     */
    public static String convertPenceToPounds(int amountInPence) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance(Locale.UK);
        return numberFormat.format(amountInPence / 100.0);
    }

}
