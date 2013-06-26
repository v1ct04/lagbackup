package com.v1ct04.ces22.lagbackup.backup.tasks;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.concurrent.AsyncTask;

import java.nio.file.Path;

public class LoadBackupTask extends AsyncTask<Backup, Void> {

    private final Path mBackupFile;

    public LoadBackupTask(Path backupFile) {
        mBackupFile = backupFile;
    }

    @Override
    protected Backup doInBackground() throws Throwable {
        return Backup.loadBackup(mBackupFile);
    }
}
