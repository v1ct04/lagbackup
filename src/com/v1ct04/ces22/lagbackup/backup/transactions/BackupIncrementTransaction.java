package com.v1ct04.ces22.lagbackup.backup.transactions;

import com.v1ct04.ces22.lagbackup.backup.exception.BackupException;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiff;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiffFolder;
import com.v1ct04.ces22.lagbackup.backup.model.BackupFile;
import com.v1ct04.ces22.lagbackup.backup.model.ModificationType;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressPublisher;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class BackupIncrementTransaction implements BackupTransaction<BackupDiff, ProgressUpdate> {

    private static final float GB_MULTIPLIER = 1024f * 1024f * 1024f;

    private final BackupDiff mToBeDiff;
    private final Set<BackupFile> mCommitedChanges = new HashSet<>();

    public BackupIncrementTransaction(BackupDiff toBeDiff) {
        mToBeDiff = toBeDiff;
    }

    @Override
    public BackupDiff commit(ProgressPublisher<ProgressUpdate> progressPublisher) throws Exception {
        assertEnoughSpace();
        int totalFiles = 0;
        for (BackupDiffFolder diffFolder : mToBeDiff.getDiffFolders())
            totalFiles += diffFolder.getFileModifications().size();
        FileProgressPublisher fileProgressPublisher =
            new FileProgressPublisher("Copiando...", totalFiles, progressPublisher);
        for (BackupDiffFolder folder : mToBeDiff.getDiffFolders()) {
            for (BackupFile file : folder.getFileModifications()) {
                if (Thread.interrupted())
                    throw new InterruptedException();
                fileProgressPublisher.publishProgress(file.getOriginalFile());
                mCommitedChanges.add(file);
                file.backupOriginalFile();
            }
        }
        return mToBeDiff;
    }

    @Override
    public void revert(ProgressPublisher <ProgressUpdate> progressPublisher) throws Exception {
        FileProgressPublisher fileProgressPublisher =
            new FileProgressPublisher("Revertendo...", mCommitedChanges.size(), progressPublisher);

        for (BackupFile file : mCommitedChanges) {
            fileProgressPublisher.publishProgress(file.getOriginalFile());
            Files.deleteIfExists(file.getBackupFile());
        }
    }

    private void assertEnoughSpace() throws IOException, BackupException {
        long availableSpace = Files.getFileStore(mToBeDiff.getBackup().getParentBackupFolder())
            .getUsableSpace();
        long requiredSpace = 0;
        for (BackupDiffFolder folder : mToBeDiff.getDiffFolders()) {
            for (BackupFile backupFile : folder.getFileModifications()) {
                if (backupFile.getModificationType() != ModificationType.DELETED)
                    requiredSpace += Files.size(backupFile.getOriginalFile());
            }
        }
        if (requiredSpace > availableSpace) {
            throw new BackupException(String.format("Não há espaço suficiente para o backup no " +
                "dispositivo de armazenamento.\n" +
                "Espaço necessário: %.2f GB\n" +
                "Espaço disponível: %.2f GB",
                requiredSpace / GB_MULTIPLIER, availableSpace / GB_MULTIPLIER));
        }
    }
}
