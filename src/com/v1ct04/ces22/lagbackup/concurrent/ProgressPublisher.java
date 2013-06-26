package com.v1ct04.ces22.lagbackup.concurrent;

public interface ProgressPublisher<ProgressType> {

    public void publishProgress(ProgressType value);
}
