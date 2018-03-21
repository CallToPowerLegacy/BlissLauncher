package org.indin.blisslaunchero.ui;

import android.content.Context;
import android.view.View;

import org.indin.blisslaunchero.utils.AppUtil;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Created by falcon on 18/3/18.
 */

public class LauncherPresenter {

    private LauncherView mLauncherView;
    private Context mContext;
    private CompositeDisposable mCompositeDisposable;

    private LauncherPresenter(Context context) {
        this.mContext = context;
        this.mCompositeDisposable = new CompositeDisposable();
    }

    public void attachView(LauncherView view) {
        this.mLauncherView = view;
    }

    public void detachView() {
        mCompositeDisposable.dispose();
        mLauncherView = null;
    }

    public boolean isViewAttached() {
        return mLauncherView != null;
    }

    public void loadApps(){
    }
}
