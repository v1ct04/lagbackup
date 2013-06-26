package com.v1ct04.ces22.lagbackup.backup.transactions;

import com.sun.nio.file.ExtendedCopyOption;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiff;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiffFolder;
import com.v1ct04.ces22.lagbackup.backup.model.BackupFile;
import com.v1ct04.ces22.lagbackup.backup.model.ModificationType;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressPublisher;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class BackupRestoreTransaction implements BackupTransaction<Void, ProgressUpdate> {

    private final BackupDiff mBackupDiff;
    private final Map<Path, Path> mBackupFolderToDestination;

    public BackupRestoreTransaction(BackupDiff backupDiff,
                                    Map<Path, Path> backupFolderToDestination) {
        mBackupDiff = backupDiff;
        mBackupFolderToDestination = backupFolderToDestination;
    }

    @Override
    public Void commit(ProgressPublisher<ProgressUpdate> progressPublisher) throws Exception {
        Map<Path, Path> filesToCopy = new HashMap<>();
        for (Path original : mBackupFolderToDestination.keySet()) {
            BackupDiffFolder diffFolder = mBackupDiff.getDiffFolder(
                mBackupDiff.getBackup().getNameFromOriginalFolder(original));
            Path restoreDest = mBackupFolderToDestination.get(original);

            Map<Path, BackupFile> snapshot = diffFolder.getFolderSnapshot();
            for (BackupFile backupFile : snapshot.values())
                if (backupFile.getModificationType() != ModificationType.DELETED) {
                    filesToCopy.put(backupFile.getBackupFile(),
                        restoreDest.resolve(original.relativize(backupFile.getOriginalFile())));
                }
        }

        FileProgressPublisher fileProgressPublisher =
            new FileProgressPublisher("Restaurando...", filesToCopy.size(), progressPublisher);
        for (Map.Entry<Path, Path> entry : filesToCopy.entrySet()) {
            if (Thread.interrupted())
                throw new InterruptedException();
            fileProgressPublisher.publishProgress(entry.getValue());
            Files.createDirectories(entry.getValue().getParent());
            Files.copy(entry.getKey(), entry.getValue(),
                StandardCopyOption.REPLACE_EXISTING, ExtendedCopyOption.INTERRUPTIBLE);
        }
        return null;
    }

    @Override
    public void revert(ProgressPublisher<ProgressUpdate> progressPublisher) throws Exception {
        throw new UnsupportedOperationException("Operação de restauração não é reversível");
    }
}
