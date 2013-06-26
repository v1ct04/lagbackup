package com.v1ct04.ces22.lagbackup.concurrent;

public class SimpleTaskCompletionListener<Type> implements AsyncTask.TaskCompletionListener<Type> {
    @Override
    public void onSuccess(Type result) {
        onFinish();
    }

    @Override
    public void onFailure(Throwable throwable) {
        onFinish();
    }

    public void onFinish() {}
}
