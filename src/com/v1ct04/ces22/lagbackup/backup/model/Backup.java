package com.v1ct04.ces22.lagbackup.backup.model;

import com.v1ct04.ces22.lagbackup.backup.exception.BackupException;
import com.v1ct04.ces22.lagbackup.backup.transactions.BackupDeleteTransaction;
import com.v1ct04.ces22.lagbackup.backup.transactions.BackupIncrementTransaction;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressPublisher;
import com.v1ct04.ces22.lagbackup.concurrent.ProgressUpdate;
import com.v1ct04.ces22.lagbackup.util.FileUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Backup {

    public static final String BACKUP_SECTION_DIVIDER = "xxx";

    private final Path mBackupFile;
    private final Path mParentBackupFolder;

    private Map<String, Path> mBackupFolderNameToOriginal = new HashMap<>();
    private Map<Path, String> mOriginalFolderToBackupName = new HashMap<>();
    private List<BackupDiff> mBackupDiffs = new LinkedList<>();

    public static Backup loadBackup(Path backupFile) throws Exception {
        try (Scanner scanner = new Scanner(backupFile, StandardCharsets.UTF_8.name())) {
            Backup backup = new Backup(backupFile,
                FileUtils.resolvePath(backupFile, scanner.nextLine()));
            if (!Files.exists(backup.getParentBackupFolder()))
                throw new BackupException("Pasta de armazenamento do backup " +
                    backup.getParentBackupFolder().getFileName() + " foi deletada!");
            while (scanner.findInLine(BACKUP_SECTION_DIVIDER) == null)
                backup.addBackupFolder(scanner.next(), scanner.nextLine().substring(1));
            scanner.nextLine();

            BackupDiff previousDiff = null;
            while (scanner.hasNext()) {
                BackupDiff backupDiff = BackupDiff.parseDiff(backup, previousDiff, scanner);
                backup.mBackupDiffs.add(backupDiff.getDiffId(), backupDiff);
                previousDiff = backupDiff;
            }
            return backup;
        }
    }

    public static Backup createBackup(Path backupFile, Collection<Path> backedUpFolders)
            throws Exception {
        if (Files.exists(backupFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new BackupException("Arquivo de backup já existe.");
        }
        Path parentBackupFolder = FileUtils.getFirstNonExistentPath(
            backupFile.resolveSibling(backupFolderNameFromFile(backupFile)));

        Backup backup = new Backup(backupFile, parentBackupFolder);
        for (Path folder : backedUpFolders)
            backup.addBackupFolder(folder);

        Files.createDirectory(parentBackupFolder);
        Files.setAttribute(parentBackupFolder, "dos:hidden", true);

        return backup;
    }

    private static String backupFolderNameFromFile(Path backupFile) {
        String fileName = backupFile.getFileName().toString().toLowerCase();
        while (fileName.startsWith("."))
            fileName = fileName.substring(1);
        fileName = fileName.replaceAll("\\.lkp$", "");
        return "." + fileName + "_store";
    }

    private Backup(Path backupFile, Path parentBackupFolder) {
        mBackupFile = backupFile;
        mParentBackupFolder = parentBackupFolder;
    }

    public Path getBackupFile() {
        return mBackupFile;
    }

    public Path getParentBackupFolder() {
        return mParentBackupFolder;
    }

    public Set<String> getBackupFolderNames() {
        return Collections.unmodifiableSet(mBackupFolderNameToOriginal.keySet());
    }

    public Collection<Path> getBackedUpFolders() {
        return Collections.unmodifiableCollection(mBackupFolderNameToOriginal.values());
    }

    public List<BackupDiff> getDiffsModifying(Collection<? extends Path> folders) {
        List<BackupDiff> backupDiffs = new LinkedList<>();
        for (BackupDiff diff : mBackupDiffs) {
            for (BackupDiffFolder diffFolder : diff.getDiffFolders()) {
                if (folders.contains(diffFolder.getOriginalFolder())) {
                    backupDiffs.add(diff);
                    break;
                }
            }
        }
        return backupDiffs;
    }

    public String getNameFromOriginalFolder(Path folder) {
        return mOriginalFolderToBackupName.get(folder);
    }

    public Path getBackupFolderByName(String name) {
        return mParentBackupFolder.resolve(name);
    }

    public Path getOriginalFolderByName(String name) {
        return mBackupFolderNameToOriginal.get(name);
    }

    public BackupDiff getDiff(int id) {
        return mBackupDiffs.get(id);
    }

    public List<BackupDiff> getDiffList() {
        return Collections.unmodifiableList(mBackupDiffs);
    }

    public BackupDiff getLastDiff() {
        if (mBackupDiffs.isEmpty())
            return null;
        return mBackupDiffs.get(mBackupDiffs.size() - 1);
    }

    public BackupIncrementTransaction incrementBackup(Collection<String> folderNames)
        throws IOException, InterruptedException {
        final BackupDiff backupDiff = BackupDiff.createDiff(this, folderNames);
        if (backupDiff.getDiffFolders().isEmpty())
            return null;
        return new MyBackupIncrementTransaction(backupDiff);
    }

    public BackupIncrementTransaction incrementBackup()
        throws IOException, InterruptedException {
        BackupDiff backupDiff = BackupDiff.createDiff(this, getBackupFolderNames());
        if (backupDiff.getDiffFolders().isEmpty())
            return null;
        return new MyBackupIncrementTransaction(backupDiff);
    }

    public BackupDeleteTransaction deleteAll() {
        return new BackupDeleteTransaction(this);
    }

    public void saveBackupFile() throws IOException {
        try (PrintStream printStream = new PrintStream(
                    Files.newOutputStream(mBackupFile, StandardOpenOption.CREATE),
                    false,
                    StandardCharsets.UTF_8.name())) {

            printStream.println(mBackupFile.relativize(mParentBackupFolder));
            for (Map.Entry<String, Path> backupFolder : mBackupFolderNameToOriginal.entrySet()) {
                printStream.printf("%s %s\n", backupFolder.getKey(), backupFolder.getValue());
            }
            printStream.println(BACKUP_SECTION_DIVIDER);
            for (BackupDiff backupDiff : mBackupDiffs) {
                backupDiff.print(printStream);
            }
            printStream.flush();
        }
    }

    // End of Public API

    private String addBackupFolder(Path folder) {
        String folderName = folder.getFileName().toString().replace(' ', '_');
        for (int i = 0; mBackupFolderNameToOriginal.containsKey(folderName); i++)
            folderName = folder.getFileName().toString() + i;
        mBackupFolderNameToOriginal.put(folderName, folder);
        mOriginalFolderToBackupName.put(folder, folderName);
        return folderName;
    }

    private void addBackupFolder(String name, String pathToOriginal) {
        Path path = Paths.get(pathToOriginal);
        mBackupFolderNameToOriginal.put(name, path);
        mOriginalFolderToBackupName.put(path, name);
    }

    private class MyBackupIncrementTransaction extends BackupIncrementTransaction {

        private final BackupDiff mBackupDiff;

        public MyBackupIncrementTransaction(BackupDiff toBeDiff) {
            super(toBeDiff);
            mBackupDiff = toBeDiff;
        }

        @Override
        public BackupDiff commit(ProgressPublisher<ProgressUpdate> progress) throws Exception {
            BackupDiff commitResult = super.commit(progress);
            mBackupDiffs.add(mBackupDiff.getDiffId(), mBackupDiff);
            return commitResult;
        }
    }
}
