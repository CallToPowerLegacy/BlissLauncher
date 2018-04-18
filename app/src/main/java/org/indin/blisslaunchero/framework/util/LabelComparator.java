package org.indin.blisslaunchero.framework.util;

import java.text.Collator;
import java.util.Comparator;

/**
 * Extension of {@link java.text.Collator} with special handling for digits. Used for comparing
 * user visible labels.
 */
public class LabelComparator implements Comparator<String> {

    private final Collator mCollator = Collator.getInstance();

    @Override
    public int compare(String titleA, String titleB) {
        // Ensure that we de-prioritize any titles that don't start with a
        // linguistic letter or digit
        boolean aStartsWithLetter = (titleA.length() > 0) &&
                Character.isLetterOrDigit(titleA.codePointAt(0));
        boolean bStartsWithLetter = (titleB.length() > 0) &&
                Character.isLetterOrDigit(titleB.codePointAt(0));
        if (aStartsWithLetter && !bStartsWithLetter) {
            return -1;
        } else if (!aStartsWithLetter && bStartsWithLetter) {
            return 1;
        }

        // Order by the title in the current locale
        return mCollator.compare(titleA, titleB);
    }
}