package com.v1ct04.ces22.lagbackup.backup.model;

import com.v1ct04.ces22.lagbackup.backup.exception.BackupException;
import com.v1ct04.ces22.lagbackup.util.FileUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class BackupDiffFolder {

    private static final String DIFF_FOLDER_DIVIDER = "fff";
    private static final String FIlE_DIFF_ENTRY = "%c %s\n";

    private final BackupDiff mBackupDiff;

    private final Path mOriginalFolder;
    private final Path mBackupFolder;
    private final int mDiffId;

    // Linked hash map to preserve iteration order
    private Map<Path, BackupFile> mFileModifications = new LinkedHashMap<>();
    private Map<Path, BackupFile> mFolderSnapshot;

    static BackupDiffFolder createDiffFolder(BackupDiff backupDiff, String folderName)
            throws IOException, InterruptedException {
        Backup backup = backupDiff.getBackup();
        BackupDiffFolder diffFolder = new BackupDiffFolder(backupDiff,
            backup.getOriginalFolderByName(folderName), backup.getBackupFolderByName(folderName));

        BackupDiffFolder previousDiffFolder = diffFolder.getLastDiffFolder();
        diffFolder.addUntrackedFiles(previousDiffFolder);
        diffFolder.addModifiedFiles(previousDiffFolder);
        diffFolder.finishCreation();
        return diffFolder;
    }

    static BackupDiffFolder parseDiffFolder(BackupDiff diff, Scanner scanner)
            throws BackupException, InterruptedException, IOException {
        Backup backup = diff.getBackup();
        String folderName = scanner.nextLine();
        Path originalFolder = backup.getOriginalFolderByName(folderName);
        Path backupFolder = backup.getBackupFolderByName(folderName);
        BackupDiffFolder diffFolder = new BackupDiffFolder(diff, originalFolder, backupFolder);

        Path lastPathRead = diffFolder.getOriginalFolder();
        while (scanner.hasNext(ModificationType.FILE_DIFF_TYPE_PATTERN)) {
            if (Thread.interrupted())
                throw new InterruptedException();
            char modType = scanner.next(ModificationType.FILE_DIFF_TYPE_PATTERN).charAt(0);
            String relativePath = scanner.nextLine().substring(1);

            Path originalFilePath = FileUtils.resolvePath(lastPathRead, relativePath);
            Path backupFilePath =
                FileUtils.resolvePath(backupFolder, originalFolder.relativize(originalFilePath));
            diffFolder.addBackupFile(new BackupFile(ModificationType.valueOfCode(modType), originalFilePath,
                backupFilePath, diffFolder.getDiffId()));
            lastPathRead = originalFilePath;
        }
        if (scanner.findInLine(DIFF_FOLDER_DIVIDER) == null)
            throw new BackupException("Arquivo de backup corrompido, impossível de processá-lo.");
        scanner.nextLine();
        diffFolder.finishCreation();

        return diffFolder;
    }

    private BackupDiffFolder(BackupDiff backupDiff, Path origFolder, Path backupFolder) {
        mBackupDiff = backupDiff;
        mOriginalFolder = origFolder;
        mBackupFolder = backupFolder;
        mDiffId = mBackupDiff.getDiffId();
    }

    public BackupDiff getBackupDiff() {
        return mBackupDiff;
    }

    public Path getOriginalFolder() {
        return mOriginalFolder;
    }

    public Path getBackupFolder() {
        return mBackupFolder;
    }

    public String getBackupFolderName() {
        return mBackupFolder.getFileName().toString();
    }

    public int getDiffId() {
        return mDiffId;
    }

    public Collection<BackupFile> getFileModifications() {
        return Collections.unmodifiableCollection(mFileModifications.values());
    }

    public Map<Path, BackupFile> getFolderSnapshot() {
        if (mFolderSnapshot != null)
            return mFolderSnapshot;

        BackupDiffFolder previousDiffFolder = getLastDiffFolder();
        if (previousDiffFolder != null) {
            mFolderSnapshot = new LinkedHashMap<>(previousDiffFolder.getFolderSnapshot());
        } else
            mFolderSnapshot = new LinkedHashMap<>();

        mFolderSnapshot.putAll(mFileModifications);
        mFolderSnapshot = Collections.unmodifiableMap(mFolderSnapshot);

        return mFolderSnapshot;
    }

    // End of Public API

    private BackupDiffFolder getLastDiffFolder() {
        return mBackupDiff.getPreviousDiffFolder(getBackupFolderName());
    }

    void print(PrintStream printStream) {
        Path lastPrinted = mOriginalFolder;
        printStream.println(mBackupFolder.getFileName());
        for (BackupFile file : mFileModifications.values()) {
            printStream.printf(FIlE_DIFF_ENTRY,
                file.getModificationType().getCode(),
                lastPrinted.relativize(file.getOriginalFile()));
            lastPrinted = file.getOriginalFile();
        }
        printStream.println(DIFF_FOLDER_DIVIDER);
        printStream.flush();
    }

    // construction utilities

    private void addUntrackedFiles(BackupDiffFolder previousDiffFolder) throws IOException {
        final Map<Path, BackupFile> folderSnapshot = previousDiffFolder == null ?
            Collections.<Path, BackupFile>emptyMap() : previousDiffFolder.getFolderSnapshot();
        Files.walkFileTree(mOriginalFolder, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
                if (Thread.interrupted())
                    throw new IOException(new InterruptedException());

                if (folderSnapshot.containsKey(file) || attrs.isDirectory())
                    return FileVisitResult.CONTINUE;
                Path backupFile =
                    FileUtils.resolvePath(mBackupFolder, mOriginalFolder.relativize(file));
                addBackupFile(BackupFile.createBackupFile(file, backupFile, mDiffId));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void addModifiedFiles(BackupDiffFolder previousDiffFolder) throws InterruptedException {
        if (previousDiffFolder == null)
            return;
        final Map<Path, BackupFile> folderSnapshot = previousDiffFolder.getFolderSnapshot();
        for (BackupFile file : folderSnapshot.values()) {
            if (Thread.interrupted())
                throw new InterruptedException();
            if (file.backupDiffers())
                addBackupFile(BackupFile.incrementBackupFile(file, mDiffId));
        }
    }

    private void addBackupFile(BackupFile backupFile) {
        mFileModifications.put(backupFile.getOriginalFile(), backupFile);
    }

    private void finishCreation() {
        mFileModifications = Collections.unmodifiableMap(mFileModifications);
    }
}
