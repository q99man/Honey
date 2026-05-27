package com.honeytong.common.upload;

import java.nio.file.Path;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.upload.images")
public record ImageUploadProperties(
        Path storagePath,
        String publicBaseUrl,
        String publicPath,
        long maxSizeBytes,
        Set<String> allowedContentTypes
) {
}
