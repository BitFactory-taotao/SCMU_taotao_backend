package com.bit.scmu_taotao.util.storage;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class ObjectKeyParser {

    private ObjectKeyParser() {
    }

    public static String extractObjectKey(String imageUrl, String bucket) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        // 前端有时可能直接传 objectKey
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            return trimLeadingSlash(imageUrl);
        }

        try {
            URI uri = URI.create(imageUrl);
            String path = URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8);
            if (path == null || path.isBlank()) {
                return null;
            }

            String normalized = trimLeadingSlash(path);

            if (bucket != null && !bucket.isBlank()) {
                String bucketPrefix = bucket + "/";
                if (normalized.startsWith(bucketPrefix)) {
                    return normalized.substring(bucketPrefix.length());
                }
            }

            return normalized;
        } catch (Exception e) {
            return null;
        }
    }

    private static String trimLeadingSlash(String value) {
        if (value == null) {
            return null;
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}