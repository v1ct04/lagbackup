package com.v1ct04.ces22.lagbackup.backup.model;

import com.v1ct04.ces22.lagbackup.backup.exception.BackupException;
import com.v1ct04.ces22.lagbackup.backup.transactions.BackupRestoreTransaction;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackupDiff {

    private static final String DIFF_HEADER = "[%d] %s\n";
    private static final Pattern DIFF_HEADER_PATTERN = Pattern.compile("\\[(\\d+)] (.+)");
    private static final DateFormat CREATION_DATE_FORMAT =
        DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US);
    private static final DateFormat TO_STRING_DATE_FORMAT =
        new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm:ss");

    private final Backup mBackup;
    private final BackupDiff mPreviousDiff;

    private final int mDiffId;
    private final Date mCreationDate;

    private final Map<String, BackupDiffFolder> mDiffFolderHashMap = new HashMap<>();

    static BackupDiff parseDiff(Backup backup, BackupDiff previousDiff, Scanner scanner)
        throws ParseException, BackupException, InterruptedException, IOException {
        String header = scanner.nextLine();
        Matcher matcher = DIFF_HEADER_PATTERN.matcher(header);
        matcher.matches();
        int diffId = Integer.parseInt(matcher.group(1));
        Date creationDate = CREATION_DATE_FORMAT.parse(matcher.group(2));

        BackupDiff backupDiff = new BackupDiff(backup, previousDiff, diffId, creationDate);
        while (scanner.findInLine(Backup.BACKUP_SECTION_DIVIDER) == null) {
            BackupDiffFolder diffFolder = BackupDiffFolder.parseDiffFolder(backupDiff, scanner);
            backupDiff.addDiffFolder(diffFolder);
        }
        scanner.nextLine();
        return backupDiff;
    }

    static BackupDiff createDiff(Backup backup, Collection<String> folderNames)
            throws IOException, InterruptedException {
        int diffId;
        BackupDiff previousBackupDiff = backup.getLastDiff();
        if (previousBackupDiff == null)
            diffId = 0;
        else
            diffId = previousBackupDiff.getDiffId() + 1;

        BackupDiff backupDiff = new BackupDiff(backup, previousBackupDiff, diffId, new Date());
        for (String backupFolderName : folderNames) {
            BackupDiffFolder diffFolder;
            diffFolder = BackupDiffFolder.createDiffFolder(backupDiff, backupFolderName);
            if (!diffFolder.getFileModifications().isEmpty() || diffId == 0)
                backupDiff.addDiffFolder(diffFolder);
        }
        return backupDiff;
    }

    private BackupDiff(Backup backup, BackupDiff previousDiff, int diffId, Date creationDate) {
        mBackup = backup;
        mPreviousDiff = previousDiff;
        mDiffId = diffId;
        mCreationDate = creationDate;
    }

    public Backup getBackup() {
        return mBackup;
    }

    public BackupDiff getPreviousDiff() {
        return mPreviousDiff;
    }

    public int getDiffId() {
        return mDiffId;
    }

    public Date getCreationDate() {
        return mCreationDate;
    }

    public Collection<BackupDiffFolder> getDiffFolders() {
        return Collections.unmodifiableCollection(mDiffFolderHashMap.values());
    }

    public BackupDiffFolder getPreviousDiffFolder(String folderName) {
        return mPreviousDiff != null ? mPreviousDiff.getDiffFolder(folderName) : null;
    }

    public BackupDiffFolder getDiffFolder(String folderName) {
        if (mDiffFolderHashMap.containsKey(folderName))
            return mDiffFolderHashMap.get(folderName);
        return mPreviousDiff != null ? mPreviousDiff.getDiffFolder(folderName) : null;
    }

    public BackupRestoreTransaction restoreBackup(Map<Path, Path> backedUpToDestination) {
        return new BackupRestoreTransaction(this, backedUpToDestination);
    }

    @Override
    public String toString() {
        return String.format("Backup %s feito dia %s",
            mPreviousDiff == null ? "completo" : ("incremental " + mDiffId),
            TO_STRING_DATE_FORMAT.format(mCreationDate));
    }

    // End of public API

    void print(PrintStream printStream) {
        printStream.printf(DIFF_HEADER, mDiffId,
            CREATION_DATE_FORMAT.format(mCreationDate));
        for (BackupDiffFolder diffFolder : mDiffFolderHashMap.values())
            diffFolder.print(printStream);
        printStream.println(Backup.BACKUP_SECTION_DIVIDER);
    }

    // construction utility
    private void addDiffFolder(BackupDiffFolder diffFolder) {
        mDiffFolderHashMap.put(diffFolder.getBackupFolderName(), diffFolder);
    }
}
