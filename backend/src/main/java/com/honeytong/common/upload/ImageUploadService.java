package com.honeytong.common.upload;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageUploadService {

    private final ImageUploadProperties properties;

    public ImageUploadService(ImageUploadProperties properties) {
        this.properties = properties;
    }

    public UploadedImageResponse storeImage(MultipartFile file, UploadImageTarget target) {
        validate(file);

        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String filename = UUID.randomUUID() + "." + extensionFor(contentType);
        Path targetDirectory = properties.storagePath().resolve(target.folderName()).normalize();
        Path targetPath = targetDirectory.resolve(filename).normalize();

        if (!targetPath.startsWith(targetDirectory)) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이미지 저장 경로가 올바르지 않습니다.");
        }

        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.EXTERNAL_SERVICE_ERROR, "이미지 저장에 실패했습니다.");
        }

        return new UploadedImageResponse(
                publicUrl(target, filename),
                file.getOriginalFilename(),
                contentType,
                file.getSize()
        );
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "업로드할 이미지 파일을 선택해주세요.");
        }
        if (file.getSize() > properties.maxSizeBytes()) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "이미지 파일 크기가 허용 범위를 초과했습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !properties.allowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "jpg, png, webp 이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new ApiException(ErrorCode.INVALID_REQUEST, "지원하지 않는 이미지 형식입니다.");
        };
    }

    private String publicUrl(UploadImageTarget target, String filename) {
        String baseUrl = trimTrailingSlash(properties.publicBaseUrl());
        String publicPath = trimSlashes(properties.publicPath());
        return baseUrl + "/" + publicPath + "/" + target.folderName() + "/" + filename;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("/+$", "");
    }

    private String trimSlashes(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
