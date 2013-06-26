package com.v1ct04.ces22.lagbackup.backup.model;

import com.sun.nio.file.ExtendedCopyOption;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BackupFile {

    private static final Pattern DIFF_REGEX = Pattern.compile("\\.diff([0-9]+)$");

    private final int mDiffId;

    private final Path mOriginalFile;
    private final Path mBackupFile;

    private final ModificationType mModificationType;

    public static BackupFile createBackupFile(Path originalFile, Path backupFile, int diffId) {
        return new BackupFile(ModificationType.CREATED, originalFile, backupFile, diffId);
    }

    public static BackupFile incrementBackupFile(BackupFile backupFile, int newDiffId) {
        if (!backupFile.getOriginalFile().toFile().exists()) {
            return new BackupFile(ModificationType.DELETED,
                backupFile.getOriginalFile(),
                backupFile.getBackupFile(),
                newDiffId);
        } else if (backupFile.getModificationType() == ModificationType.DELETED) {
            return new BackupFile(ModificationType.CREATED,
                backupFile.getOriginalFile(),
                backupFile.getBackupFile(),
                newDiffId);
        }
        return new BackupFile(ModificationType.MODIFIED,
            backupFile.getOriginalFile(),
            backupFile.getBackupFile(),
            newDiffId);
    }

    public BackupFile(ModificationType type, Path originalFile, Path backupFile, int diffId) {
        if (!originalFile.isAbsolute() ||
            (type != ModificationType.DELETED && !backupFile.isAbsolute())) {
            throw new IllegalArgumentException("Both paths must be absolute to create BackupFile," +
                "unless the modification type is DELETED, when the backupFile only can be null.");
        }
        if (type == ModificationType.DELETED)
            mBackupFile = backupFile;
        else {
            String backupFileName = backupFile.toString();
            Matcher regMatcher = DIFF_REGEX.matcher(backupFileName);
            if (regMatcher.find()) {
                backupFileName = backupFileName.substring(0, regMatcher.start());
            }
            mBackupFile = Paths.get(backupFileName.concat(".diff" + diffId));
        }
        mOriginalFile = originalFile;
        mModificationType = type;
        mDiffId = diffId;
    }

    // beans

    public int getDiffId() {
        return mDiffId;
    }

    public Path getOriginalFile() {
        return mOriginalFile;
    }

    public Path getBackupFile() {
        return mBackupFile;
    }

    public ModificationType getModificationType() {
        return mModificationType;
    }

    // utility

    public long getOriginalFileLastModified() {
        try {
            return Files.getLastModifiedTime(mOriginalFile, LinkOption.NOFOLLOW_LINKS).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }

    public long getBackupFileLastModified() {
        try {
            return Files.getLastModifiedTime(mBackupFile, LinkOption.NOFOLLOW_LINKS).toMillis();
        } catch (IOException | NullPointerException e) {
            return 0;
        }
    }

    public boolean backupDiffers() {
        if (mModificationType == ModificationType.DELETED)
            return Files.exists(mOriginalFile, LinkOption.NOFOLLOW_LINKS);

        // some OSs differ in file time precision, so we use a 5 seconds unit resolution
        return !Files.exists(mOriginalFile, LinkOption.NOFOLLOW_LINKS) ||
            Math.abs(getOriginalFileLastModified() - getBackupFileLastModified()) > 5000;
    }

    public void backupOriginalFile() throws IOException {
        if (mModificationType == ModificationType.DELETED)
            return;
        Files.createDirectories(mBackupFile.getParent());
        Files.copy(mOriginalFile, mBackupFile,
            StandardCopyOption.REPLACE_EXISTING, ExtendedCopyOption.INTERRUPTIBLE);
    }

    @Override
    public String toString() {
        return mOriginalFile.toString();
    }

    // this breaks the equals contract a little bit by returning true when o is an instance of Path
    // and it is equal to this BackupFile original file path (breaks reflexivity). Does not break
    // the hashCode and equals relation though.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o instanceof BackupFile) {
            BackupFile that = (BackupFile) o;
            return mOriginalFile.equals(that.mOriginalFile);
        } else if (o instanceof Path) {
            return mOriginalFile.equals(o);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mOriginalFile.hashCode();
    }
}
