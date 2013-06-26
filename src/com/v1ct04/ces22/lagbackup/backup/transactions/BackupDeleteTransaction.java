package com.v1ct04.ces22.lagbackup.backup.transactions;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressPublisher;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class BackupDeleteTransaction implements BackupTransaction<Void, ProgressUpdate> {

    private final Backup mBackup;
    private int mTotalFiles = 0;

    public BackupDeleteTransaction(Backup backup) {
        mBackup = backup;
    }

    @Override
    public Void commit(ProgressPublisher<ProgressUpdate> progressPublisher) throws Exception {
        Path parentBackupFolder = mBackup.getParentBackupFolder();
        mTotalFiles = 0;
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
                Files.delete(file);
                fileProgressPublisher.publishProgress(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        Files.deleteIfExists(mBackup.getBackupFile());

        return null;
    }

    @Override
    public void revert(ProgressPublisher<ProgressUpdate> progressPublisher) throws Exception {
        throw new UnsupportedOperationException("Deleção de backup não é uma operação reversível");
    }
}
