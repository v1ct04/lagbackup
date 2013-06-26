package com.v1ct04.ces22.lagbackup.backup.tasks;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.concurrent.AsyncTask;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

public class DeleteBackupTask extends AsyncTask<Void, ProgressUpdate> {

    private final Backup mBackup;

    public DeleteBackupTask(Backup backup) {
        mBackup = backup;
    }

    @Override
    protected Void doInBackground() throws Throwable {
        publishProgress(new ProgressUpdate("Inicializando...", 0));
        mBackup.deleteAll().commit(this);
        return null;
    }
}
