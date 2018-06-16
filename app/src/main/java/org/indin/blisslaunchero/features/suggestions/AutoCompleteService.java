package org.indin.blisslaunchero.features.suggestions;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AutoCompleteService {

    @GET("/ac/")
    Observable<List<AutoCompleteServiceRawResult>> query(@Query("q") String query);
}
