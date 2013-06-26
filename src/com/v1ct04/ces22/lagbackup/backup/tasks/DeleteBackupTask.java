package com.v1ct04.ces22.lagbackup.backup.tasks;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.concurrent.AsyncTask;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

public class DeleteBackupTask extends AsyncTask<Boolean, ProgressUpdate> {

    private final Backup mBackup;

    public DeleteBackupTask(Backup backup) {
        mBackup = backup;
    }

    @Override
    protected Boolean doInBackground() throws Throwable {
        publishProgress(new ProgressUpdate("Inicializando...", 0));
        return mBackup.deleteAll().commit(this);
    }
}
