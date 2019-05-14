package foundation.e.blisslauncher.features.suggestions;

import io.reactivex.Single;

public interface SuggestionProvider {

    Single<SuggestionsResult> query(String query);
}
