package foundation.e.blisslauncher.features.launcher;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import foundation.e.blisslauncher.R;
import foundation.e.blisslauncher.core.customviews.BlissFrameLayout;
import foundation.e.blisslauncher.core.database.model.LauncherItem;
import foundation.e.blisslauncher.features.suggestions.AutoCompleteAdapter;
import foundation.e.blisslauncher.features.suggestions.AutoCompleteServiceResult;
import io.reactivex.observers.DisposableObserver;

public class SearchInputDisposableObserver extends DisposableObserver<AutoCompleteServiceResult> {

    private AutoCompleteAdapter networkSuggestionAdapter;
    private LauncherActivity launcherActivity;
    private ViewGroup appSuggestionsViewGroup;

    public SearchInputDisposableObserver(LauncherActivity activity, RecyclerView.Adapter autoCompleteAdapter, ViewGroup viewGroup) {
        this.networkSuggestionAdapter = (AutoCompleteAdapter) autoCompleteAdapter;
        this.launcherActivity = activity;
        this.appSuggestionsViewGroup = viewGroup;
    }

    @Override
    public void onNext(AutoCompleteServiceResult autoCompleteServiceResults) {
        if (autoCompleteServiceResults.type
                == AutoCompleteServiceResult.TYPE_NETWORK_ITEM) {
            List<String> suggestions = new ArrayList<>();
            for (int i = 0;
                    i < (autoCompleteServiceResults.networkItems.size() > 5 ? 5
                            : autoCompleteServiceResults.networkItems.size());
                    i++) {
                suggestions.add(
                        autoCompleteServiceResults.networkItems.get(i).getPhrase());
            }
            networkSuggestionAdapter.updateSuggestions(suggestions,
                    autoCompleteServiceResults.queryText);
        } else if (autoCompleteServiceResults.type
                == AutoCompleteServiceResult.TYPE_LAUNCHER_ITEM) {
            ((ViewGroup)appSuggestionsViewGroup.findViewById(R.id.suggestedAppGrid)).removeAllViews();
            appSuggestionsViewGroup.findViewById(R.id.openUsageAccessSettings).setVisibility(View.GONE);
            appSuggestionsViewGroup.findViewById(R.id.suggestedAppGrid).setVisibility(View.VISIBLE);
            for (LauncherItem launcherItem : autoCompleteServiceResults
                    .getLauncherItems()) {
                BlissFrameLayout blissFrameLayout = launcherActivity.prepareSuggestedApp(
                        launcherItem);
                launcherActivity.addAppToGrid(appSuggestionsViewGroup.findViewById(R.id.suggestedAppGrid), blissFrameLayout);
            }
        } else {
            launcherActivity.refreshSuggestedApps(appSuggestionsViewGroup,true);
            networkSuggestionAdapter.updateSuggestions(new ArrayList<>(),
                    autoCompleteServiceResults.queryText);
        }
    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
    }

    @Override
    public void onComplete() {
    }
}
