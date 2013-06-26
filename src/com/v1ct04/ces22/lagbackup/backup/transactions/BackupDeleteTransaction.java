package com.v1ct04.ces22.lagbackup.backup.transactions;

import com.v1ct04.ces22.lagbackup.backup.model.Backup;
import com.v1ct04.ces22.lagbackup.concurrent.Parallel;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressPublisher;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

public class BackupDeleteTransaction implements BackupTransaction<Boolean, ProgressUpdate> {

    private final Backup mBackup;

    private boolean mAllFilesDeleted;

    public BackupDeleteTransaction(Backup backup) {
        mBackup = backup;
    }

    @Override
    public Boolean commit(ProgressPublisher<ProgressUpdate> progressPublisher) throws Exception {
        Path parentBackupFolder = mBackup.getParentBackupFolder();
        mAllFilesDeleted = true;
        final Set<Path> toBeDeleted = new HashSet<>();
        Files.walkFileTree(parentBackupFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                toBeDeleted.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        final FileProgressPublisher fileProgressPublisher =
            new FileProgressPublisher("Deletando...", toBeDeleted.size(), progressPublisher);
        Files.setAttribute(parentBackupFolder, "dos:hidden", false);
        Parallel.forEach(toBeDeleted, new Parallel.Operation<Path>() {
            @Override
            public void Do(Path item) throws Exception {
                fileProgressPublisher.publishProgress(item);
                try {
                    Files.delete(item);
                } catch (AccessDeniedException ex) {
                    ex.printStackTrace();
                    mAllFilesDeleted = false;
                }
            }
        });
        progressPublisher.publishProgress(new ProgressUpdate("Finalizando...", 1));
        Files.walkFileTree(parentBackupFolder, new SimpleFileVisitor<Path>() {
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
