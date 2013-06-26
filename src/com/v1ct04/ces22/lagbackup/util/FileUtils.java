package com.v1ct04.ces22.lagbackup.util;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public class FileUtils {

    public static Path getFirstNonExistentPath(Path basePathName) {
        if (!Files.exists(basePathName, LinkOption.NOFOLLOW_LINKS))
            return basePathName;

        String fullFileName = basePathName.getFileName().toString();
        String fileName;
        String extension;
        int index = fullFileName.indexOf('.', 1);
        if (index > 0) {
            extension = fullFileName.substring(index);
            fileName = fullFileName.substring(0, index);
        } else {
            extension = "";
            fileName = fullFileName;
        }
        for (int i = 0; Files.exists(basePathName, LinkOption.NOFOLLOW_LINKS); i++) {
            basePathName = basePathName.resolveSibling(fileName + i + extension);
        }
        return basePathName;
    }

    public static Path resolvePath(Path basePath, String relative) {
        while (relative.startsWith("..")) {
            basePath = basePath.getParent();
            relative = relative.substring(3);
        }
        return basePath.resolve(relative);
    }

    public static Path resolvePath(Path basePath, Path relative) {
        return resolvePath(basePath, relative.toString());
    }
}
