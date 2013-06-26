package com.v1ct04.ces22.lagbackup.backup.transactions;

import com.v1ct04.ces22.lagbackup.backup.exception.BackupException;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiff;
import com.v1ct04.ces22.lagbackup.backup.model.BackupDiffFolder;
import com.v1ct04.ces22.lagbackup.backup.model.BackupFile;
import com.v1ct04.ces22.lagbackup.backup.model.ModificationType;
import com.v1ct04.ces22.lagbackup.concurrent.Parallel;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressPublisher;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.util.*;

public class BackupIncrementTransaction implements BackupTransaction<BackupDiff, ProgressUpdate> {

    private static final float GB_MULTIPLIER = 1024f * 1024f * 1024f;

    private final BackupDiff mToBeDiff;
    private final Set<BackupFile> mCommitedChanges = new LinkedHashSet<>();

    public BackupIncrementTransaction(BackupDiff toBeDiff) {
        mToBeDiff = toBeDiff;
    }

    @Override
    public BackupDiff commit(ProgressPublisher<ProgressUpdate> progressPublisher) throws Exception {
        Set<BackupFile> toBeCopied = new HashSet<>();
        for (BackupDiffFolder folder : mToBeDiff.getDiffFolders()) {
            toBeCopied.addAll(folder.getFileModifications());
        }
        assertEnoughSpace(toBeCopied);
        int totalFiles = toBeCopied.size();
        final FileProgressPublisher fileProgressPublisher =
            new FileProgressPublisher("Copiando...", totalFiles, progressPublisher);
        Parallel.forEach(toBeCopied, new Parallel.Operation<BackupFile>() {
            @Override
            public void Do(BackupFile file) throws Exception {
                fileProgressPublisher.publishProgress(file.getOriginalFile());
                mCommitedChanges.add(file);
                file.backupOriginalFile();
            }
        });
        return mToBeDiff;
    }

    @Override
    public void revert(ProgressPublisher <ProgressUpdate> progressPublisher) throws Exception {
        final FileProgressPublisher fileProgressPublisher =
            new FileProgressPublisher("Revertendo...", mCommitedChanges.size(), progressPublisher);

        Parallel.forEach(mCommitedChanges, new Parallel.Operation<BackupFile>() {
            @Override
            public void Do(BackupFile file) throws Exception {
                try {
                    fileProgressPublisher.publishProgress(file.getOriginalFile());
                    Files.deleteIfExists(file.getBackupFile());
                } catch (AccessDeniedException ex) {
                    // do nothing, some files just can't be deleted :(
                    ex.printStackTrace();
                }
            }
        });
    }

    private void assertEnoughSpace(Set<BackupFile> toBeCopied) throws IOException, BackupException {
        long availableSpace =
            Files.getFileStore(mToBeDiff.getBackup().getParentBackupFolder()).getUsableSpace();
        long requiredSpace = 0;
        for (BackupFile backupFile : toBeCopied) {
            if (backupFile.getModificationType() != ModificationType.DELETED)
                requiredSpace += backupFile.getOriginalFile().toFile().length();
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
