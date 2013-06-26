package com.v1ct04.ces22.lagbackup.concurrent;

import java.util.ArrayList;
import java.util.List;

public abstract class AsyncTask<ReturnType, ProgressType> extends Thread
    implements ProgressPublisher<ProgressType> {

    private List<OnProgressListener<ProgressType>> mProgressListeners = new ArrayList<>();
    private List<TaskCompletionListener<ReturnType>> mTaskCompletionListeners = new ArrayList<>();

    protected abstract ReturnType doInBackground() throws Throwable;

    private ReturnType mSuccessfulResult;
    private Exception mExceptionThrown;

    @Override
    public void run() {
        try {
            ReturnType result = doInBackground();
            synchronized (this) {
                mSuccessfulResult = result;
                for (TaskCompletionListener<ReturnType> listener : mTaskCompletionListeners)
                    listener.onSuccess(mSuccessfulResult);
            }
        } catch (Exception e) {
            synchronized (this) {
                mExceptionThrown = e;
                for (TaskCompletionListener listener : mTaskCompletionListeners)
                    listener.onFailure(mExceptionThrown);
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public synchronized void addProgressListener(OnProgressListener<ProgressType> listener) {
        mProgressListeners.add(listener);
    }

    public synchronized void addTaskCompletionListener(TaskCompletionListener<ReturnType> listener) {
        mTaskCompletionListeners.add(listener);
        if (mSuccessfulResult != null)
            listener.onSuccess(mSuccessfulResult);
        else if (mExceptionThrown != null)
            listener.onFailure(mExceptionThrown);
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
        public void onSuccess(ReturnType returnType);

        public void onFailure(Exception throwable);
    }
}
