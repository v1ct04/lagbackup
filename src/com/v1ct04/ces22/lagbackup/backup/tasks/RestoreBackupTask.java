package com.v1ct04.ces22.lagbackup.backup.tasks;

import com.v1ct04.ces22.lagbackup.backup.model.BackupDiff;
import com.v1ct04.ces22.lagbackup.backup.transactions.BackupRestoreTransaction;
import com.v1ct04.ces22.lagbackup.concurrent.AsyncTask;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.Map;

public class RestoreBackupTask extends AsyncTask<Void, ProgressUpdate> {

    private final BackupDiff mBackupDiff;
    private final Map<Path, Path> mBackupToDestination;

    public RestoreBackupTask(BackupDiff backupDiff, Map<Path, Path> backupToDestination) {
        mBackupDiff = backupDiff;
        mBackupToDestination = backupToDestination;
    }

    @Override
    protected Void doInBackground() throws Throwable {
        publishProgress(new ProgressUpdate("Inicializando...", 0));
        try {
            BackupRestoreTransaction transaction = mBackupDiff.restoreBackup(mBackupToDestination);
            transaction.commit(this);
            return null;
        } catch (FileSystemException ex) {
            if (ex.getMessage().contains("aborted"))
                throw new InterruptedException();
            throw ex;
        }
    }
}
