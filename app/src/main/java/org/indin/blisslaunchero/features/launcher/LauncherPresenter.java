package org.indin.blisslaunchero.features.launcher;

import android.content.Context;
import android.util.ArrayMap;
import android.util.Log;

import org.indin.blisslaunchero.framework.database.model.AppItem;
import org.indin.blisslaunchero.framework.mvp.MvpPresenter;
import org.indin.blisslaunchero.framework.utils.AppUtils;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class LauncherPresenter extends MvpPresenter<LauncherContract.View> implements
        LauncherContract.Presenter {
    private CompositeDisposable mCompositeDisposable;

    private static LauncherPresenter sInstance;

    private AllAppsList mAllAppsList;

    private static final String TAG = "LauncherPresenter";
    private boolean isRequesting = false;

    public LauncherPresenter() {
        mCompositeDisposable = new CompositeDisposable();
    }

    public static LauncherPresenter getInstance() {
        if(sInstance == null){
            sInstance = new LauncherPresenter();
        }
        return sInstance;
    }

    @Override
    public void attachView(LauncherContract.View view) {
        super.attachView(view);
        if(mAllAppsList != null){
            getView().showApps(mAllAppsList.launchableApps,
                    mAllAppsList.pinnedApps);
        }
    }

    @Override
    public void detachView() {
        super.detachView();
        mCompositeDisposable.dispose();
    }

    @Override
    public void loadApps(Context context) {
        if(!isRequesting){
            mCompositeDisposable.add(loadAppsAndIconCache(context)
                    .doOnSubscribe(__ -> isRequesting = true)
                    .doOnTerminate(() -> isRequesting = false)
                    .map(appItemList -> {
                        ArrayMap<String, AppItem> itemArrayMap = new ArrayMap<>();
                        for (AppItem appItem : appItemList) {
                            itemArrayMap.put(appItem.getComponentName(), appItem);
                        }
                        return itemArrayMap;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableObserver<ArrayMap<String, AppItem>>() {
                        @Override
                        public void onNext(ArrayMap<String, AppItem> appItemArrayMap) {
                            if(isViewAttached()){
                                getView().showApps(mAllAppsList.launchableApps,
                                        mAllAppsList.pinnedApps);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(TAG, "onError: ", e);
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "onComplete() called");
                        }
                    }));
        }else{
            Log.i(TAG, "loadApps: already requesting");
        }

    }

    @Override
    public void getApps(Context context) {
        if(mAllAppsList == null && !isRequesting){
            loadApps(context);
        }else if(mAllAppsList != null){
            getView().showApps(mAllAppsList.launchableApps,
                    mAllAppsList.pinnedApps);
        }
    }

    private Observable<List<AppItem>> loadAppsAndIconCache(Context context) {
        return Observable.defer(
                () -> Observable.just(AppUtils.loadAll(context)));
    }
}
