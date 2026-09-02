package com.bit.scmu_taotao.util.storage;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public final class ObjectKeyGenerator {

    private ObjectKeyGenerator() {
    }

    public static String forUserImage(String userId, String originalFilename) {
        String ext = extractExt(originalFilename);
        LocalDate now = LocalDate.now();
        String safeUserId = userId == null ? "anonymous" : userId.replaceAll("[^a-zA-Z0-9_-]", "_");
        return String.format(
                "user/%s/%d/%02d/%02d/%s.%s",
                safeUserId,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                UUID.randomUUID().toString().replace("-", ""),
                ext
        );
    }

    public static String extractExt(String filename) {
        return extractExt(filename, "bin");
    }

    public static String extractExt(String filename, String defaultExt) {
        if (filename == null || !filename.contains(".")) {
            return defaultExt;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return ext.isBlank() ? defaultExt : ext;
    }
}
