package com.v1ct04.ces22.lagbackup.concurrent;

import com.v1ct04.ces22.lagbackup.concurrent.AsyncTask;

public class SimpleTaskCompletionListener<Type> implements AsyncTask.TaskCompletionListener<Type> {
    @Override
    public void onSuccess(Type o) {
        onFinish();
    }

    @Override
    public void onFailure(Exception throwable) {
        onFinish();
    }

    public void onFinish() {}
}
