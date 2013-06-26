package com.v1ct04.ces22.lagbackup.backup.transactions;

import com.v1ct04.ces22.lagbackup.concurrent.ProgressPublisher;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.nio.file.Path;

public class FileProgressPublisher implements ProgressPublisher<Path> {

    private String mMessage;
    private ProgressPublisher<ProgressUpdate> mInnerPublisher;
    private float mTotalFiles;
    private int mCopiedFiles;

    public FileProgressPublisher(String message,
                                 int totalFiles,
                                 ProgressPublisher<ProgressUpdate> innerPublisher) {
        mMessage = message;
        mInnerPublisher = innerPublisher;
        mTotalFiles = totalFiles;
        mCopiedFiles = 0;
    }

    @Override
    public void publishProgress(Path value) {
        mInnerPublisher.publishProgress(
            new ProgressUpdate(mMessage + " " + value, mCopiedFiles++ / mTotalFiles));
    }
}
