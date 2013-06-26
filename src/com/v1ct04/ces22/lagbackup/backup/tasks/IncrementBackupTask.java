package com.v1ct04.ces22.lagbackup.backup.tasks;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiff;
import com.v1ct04.ces22.lagbackup.backup.transactions.BackupIncrementTransaction;
import com.v1ct04.ces22.lagbackup.concurrent.AsyncTask;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class IncrementBackupTask extends AsyncTask<BackupDiff, ProgressUpdate> {

    private final Backup mBackup;
    private final Set<String> mFolderNames;

    public IncrementBackupTask(Backup backup, Collection<Path> originalFolderNames) {
        mBackup = backup;
        mFolderNames = new HashSet<>(originalFolderNames.size());
        for (Path folder : originalFolderNames)
            mFolderNames.add(backup.getNameFromOriginalFolder(folder));
    }

    @Override
    protected BackupDiff doInBackground() throws Throwable {
        publishProgress(new ProgressUpdate("Inicializando...", 0));
        BackupIncrementTransaction transaction = mBackup.incrementBackup(mFolderNames);
        if (transaction == null)
            return null;
        try {
            BackupDiff diff = transaction.commit(this);
            publishProgress(new ProgressUpdate("Finalizando...", 1));
            mBackup.saveBackupFile();
            return diff;
        } catch (Exception ex) {
            publishProgress(new ProgressUpdate("Erro encontrado. Revertendo...", 0));
            transaction.revert(this);
            if (ex instanceof FileSystemException && ex.getMessage().contains("aborted"))
                throw new InterruptedException();
            throw ex;
        }
    }
}
