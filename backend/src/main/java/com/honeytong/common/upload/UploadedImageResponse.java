package com.honeytong.common.upload;

public record UploadedImageResponse(
        String imageUrl,
        String originalFilename,
        String contentType,
        long size
) {
}
