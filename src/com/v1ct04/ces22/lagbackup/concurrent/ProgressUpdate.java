package com.v1ct04.ces22.lagbackup.concurrent;

public class ProgressUpdate {
    private final String mMessage;
    private final float mCompletionRatio;

    public ProgressUpdate(String message, float completionRatio) {
        mMessage = message;
        mCompletionRatio = completionRatio;
    }

    public String getMessage() {
        return mMessage;
    }

    public float getCompletionRatio() {
        return mCompletionRatio;
    }
}
