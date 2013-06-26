package com.v1ct04.ces22.lagbackup.concurrent;

import javafx.application.Platform;

public class FXThreadTaskListener<Type, ProgressType> extends SimpleTaskCompletionListener<Type>
        implements AsyncTask.OnProgressListener<ProgressType> {

    @Override
    public final void onSuccess(final Type o) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                FXThreadTaskListener.super.onSuccess(o);
                onFXThreadSuccess(o);
            }
        });
    }

    @Override
    public final void onFailure(final Exception throwable) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                FXThreadTaskListener.super.onFailure(throwable);
                onFXThreadFailure(throwable);
            }
        });
    }

    @Override
    public final void onProgressUpdate(final ProgressType value) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                onFXThreadProgressUpdate(value);
            }
        });
    }

    public void onFXThreadSuccess(Type o) {}

    public void onFXThreadFailure(Exception throwable) {}

    public void onFXThreadProgressUpdate(ProgressType value) {}
}
