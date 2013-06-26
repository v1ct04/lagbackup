package com.v1ct04.ces22.lagbackup.backup.transactions;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressPublisher;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class BackupDeleteTransaction implements BackupTransaction<Boolean, ProgressUpdate> {

    private final Backup mBackup;
    private int mTotalFiles = 0;
    private boolean mAllFilesDeleted;

    public BackupDeleteTransaction(Backup backup) {
        mBackup = backup;
    }

    @Override
    public Boolean commit(ProgressPublisher<ProgressUpdate> progressPublisher) throws Exception {
        Path parentBackupFolder = mBackup.getParentBackupFolder();
        mTotalFiles = 0;
        mAllFilesDeleted = true;
        Files.walkFileTree(parentBackupFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                mTotalFiles++;
                return FileVisitResult.CONTINUE;
            }
        });
        final FileProgressPublisher fileProgressPublisher =
            new FileProgressPublisher("Deletando...", mTotalFiles, progressPublisher);
        Files.setAttribute(parentBackupFolder, "dos:hidden", false);
        Files.walkFileTree(parentBackupFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                fileProgressPublisher.publishProgress(file);
                try {
                    Files.delete(file);
                } catch (AccessDeniedException ex) {
                    ex.printStackTrace();
                    mAllFilesDeleted = false;
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
                try {
                    Files.delete(dir);
                } catch (DirectoryNotEmptyException ex) {
                    // some file inside could not be deleted, we can do nothing. just log
                    ex.printStackTrace();
                }
                return FileVisitResult.CONTINUE;
            }
        });
        Files.deleteIfExists(mBackup.getBackupFile());

        return mAllFilesDeleted;
    }

    @Override
    public void revert(ProgressPublisher<ProgressUpdate> progressPublisher) throws Exception {
        throw new UnsupportedOperationException("Deleção de backup não é uma operação reversível");
    }
}
