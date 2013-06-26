package com.v1ct04.ces22.lagbackup.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public abstract class AsyncTask<ReturnType, ProgressType>
        implements ProgressPublisher<ProgressType> {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private List<OnProgressListener<ProgressType>> mProgressListeners = new ArrayList<>();
    private List<TaskCompletionListener<ReturnType>> mTaskCompletionListeners = new ArrayList<>();

    private Future mRunningTask;

    private ReturnType mSuccessfulResult;
    private Throwable mFailureThrown;

    public void start() {
        if (mRunningTask != null)
            throw new IllegalStateException("AsyncTask has already been started.");
        mRunningTask = EXECUTOR_SERVICE.submit(new TaskRunnable());
    }

    public void cancel() {
        if (mRunningTask != null)
            mRunningTask.cancel(true);
    }

    protected abstract ReturnType doInBackground() throws Throwable;

    public synchronized void addProgressListener(OnProgressListener<ProgressType> listener) {
        mProgressListeners.add(listener);
    }

    public synchronized void addTaskCompletionListener(TaskCompletionListener<ReturnType> listener) {
        mTaskCompletionListeners.add(listener);
        if (mSuccessfulResult != null)
            listener.onSuccess(mSuccessfulResult);
        else if (mFailureThrown != null)
            listener.onFailure(mFailureThrown);
    }

    @Override
    public void publishProgress(ProgressType value) {
        for (OnProgressListener<ProgressType> listener : mProgressListeners)
            listener.onProgressUpdate(value);
    }

    public interface OnProgressListener<ProgressType> {
        public void onProgressUpdate(ProgressType value);
    }

    public interface TaskCompletionListener<ReturnType> {
        public void onSuccess(ReturnType result);

        public void onFailure(Throwable throwable);
    }

    private class TaskRunnable implements Runnable {
        @Override
        public void run() {
            try {
                ReturnType result = doInBackground();
                synchronized (this) {
                    mSuccessfulResult = result;
                    for (TaskCompletionListener<ReturnType> listener : mTaskCompletionListeners)
                        listener.onSuccess(mSuccessfulResult);
                }
            } catch (Throwable t) {
                synchronized (this) {
                    mFailureThrown = t;
                    for (TaskCompletionListener<ReturnType> listener : mTaskCompletionListeners)
                        listener.onFailure(mFailureThrown);
                }
            }
        }
    }
}
