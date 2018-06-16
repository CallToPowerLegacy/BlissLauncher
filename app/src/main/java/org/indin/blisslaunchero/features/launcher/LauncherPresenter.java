package org.indin.blisslaunchero.features.launcher;

import android.content.Context;
import android.util.Log;

import org.indin.blisslaunchero.framework.mvp.MvpPresenter;
import org.indin.blisslaunchero.framework.util.AppUtil;
import org.indin.blisslaunchero.framework.util.IconPackUtil;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class LauncherPresenter extends MvpPresenter<LauncherContract.View> implements
        LauncherContract.Presenter {
    private CompositeDisposable mCompositeDisposable;

    private static final String TAG = "LauncherPresenter";

    public LauncherPresenter() {

    }

    @Override
    public void attachView(LauncherContract.View view) {
        super.attachView(view);
        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void detachView() {
        super.detachView();
        mCompositeDisposable.dispose();
        mCompositeDisposable = null;
    }

    @Override
    public void loadApps(Context context) {

        checkViewAttached();
        mCompositeDisposable.add(loadAppsAndIconCache(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<AllAppsList>() {
                    @Override
                    public void onNext(AllAppsList allAppsList) {
                        getView().showApps(allAppsList.launchableApps,
                                allAppsList.pinnedApps);
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
    }

    private Observable<AllAppsList> loadAppsAndIconCache(Context context) {
        //IconPackUtil.cacheIconsFromIconPack(context);
        return Observable.defer(
                () -> Observable.just(AppUtil.loadAll(context)));
    }
}
