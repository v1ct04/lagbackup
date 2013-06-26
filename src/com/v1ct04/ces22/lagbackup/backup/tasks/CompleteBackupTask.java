package com.v1ct04.ces22.lagbackup.backup.tasks;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.backup.transactions.BackupIncrementTransaction;
import com.v1ct04.ces22.lagbackup.concurrent.AsyncTask;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.io.File;
import java.nio.file.FileSystemException;
import java.nio.file.Path;
import java.util.Collection;

public class CompleteBackupTask extends AsyncTask<Backup, ProgressUpdate> {

    private final Path mBackupFile;
    private final Collection<Path> mBackedUpFolders;

    public CompleteBackupTask(Path backupFile, Collection<Path> backedUpFolders) {
        if (backedUpFolders == null || backedUpFolders.isEmpty())
            throw new IllegalArgumentException("There must be at least one backed up folder");
        for (Path path : backedUpFolders) {
            File file = path.toFile();
            if (!file.exists() || !file.isDirectory())
                throw new IllegalArgumentException("All files to be backed up must exist and" +
                    " be directories");
        }
        mBackupFile = backupFile;
        mBackedUpFolders = backedUpFolders;
    }

    @Override
    protected Backup doInBackground() throws Throwable {
        Backup backup = null;
        BackupIncrementTransaction transaction = null;
        try {
            publishProgress(new ProgressUpdate("Inicializando...", 0));
            backup = Backup.createBackup(mBackupFile, mBackedUpFolders);
            backup.saveBackupFile();

            transaction = backup.incrementBackup();
            transaction.commit(this);

            publishProgress(new ProgressUpdate("Finalizando...", 1));
            backup.saveBackupFile();
            return backup;
        } catch (Exception ex) {
            publishProgress(new ProgressUpdate("Erro encontrado. Revertendo...", 0));
            if (transaction != null)
                transaction.revert(this);
            if (backup != null)
                backup.deleteAll().commit(this);

            if (ex instanceof FileSystemException && ex.getMessage().contains("aborted"))
                throw new InterruptedException();
            throw ex;
        }
    }
}
