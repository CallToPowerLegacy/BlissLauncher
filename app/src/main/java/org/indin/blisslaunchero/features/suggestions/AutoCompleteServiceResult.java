package org.indin.blisslaunchero.features.suggestions;

import java.util.List;

public class AutoCompleteServiceResult {
    public List<AutoCompleteServiceRawResult> items;
    public String queryText;

    public AutoCompleteServiceResult(
            List<AutoCompleteServiceRawResult> items, String queryText) {
        this.items = items;
        this.queryText = queryText;
    }
}
