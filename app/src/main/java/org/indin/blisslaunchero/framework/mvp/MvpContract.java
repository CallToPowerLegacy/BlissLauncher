package org.indin.blisslaunchero.framework.mvp;

public interface MvpContract {

    interface View {
    }

    interface Presenter<V extends View> {
        void attachView(V view);
        void resume();
        void pause();
        void detachView();
    }
}
