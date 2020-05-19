package foundation.e.blisslauncher.features.suggestions;

import java.io.IOException;

import foundation.e.blisslauncher.core.network.RetrofitService;
import foundation.e.blisslauncher.core.network.qwant.QwantItem;
import foundation.e.blisslauncher.core.network.qwant.QwantResult;
import foundation.e.blisslauncher.core.network.qwant.QwantSuggestionService;
import io.reactivex.Observable;
import io.reactivex.Single;

public class QwantProvider implements SuggestionProvider {

    private QwantSuggestionService getSuggestionService() {
        String URL = "https://api.qwant.com";
        return RetrofitService.getInstance(
                URL).create(QwantSuggestionService.class);
    }

    @Override
    public Single<SuggestionsResult> query(String query) {
        return getSuggestionService().query(query)
                .retryWhen(errors -> errors.flatMap(error -> {
                    // For IOExceptions, we  retry
                    if (error instanceof IOException) {
                        return Observable.just(new QwantResult());
                    }
                    // For anything else, don't retry
                    return Observable.error(error);
                }))
                .filter(qwantResult -> qwantResult.getStatus().equals("success"))
                .flatMapIterable(qwantResult -> qwantResult.getData().getItems())
                .take(3)
                .map(QwantItem::getValue)
                .toList()
                .map(suggestions -> {
                    SuggestionsResult suggestionsResult =
                            new SuggestionsResult(query);
                    suggestionsResult.setNetworkItems(suggestions);
                    return suggestionsResult;
                });
    }
}
