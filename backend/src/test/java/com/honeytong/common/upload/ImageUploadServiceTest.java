package com.honeytong.common.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.honeytong.common.error.ApiException;
import com.honeytong.common.error.ErrorCode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void storeImage_savesFileAndReturnsPublicUrl() throws Exception {
        ImageUploadService service = new ImageUploadService(
                new ImageUploadProperties(
                        tempDir,
                        "http://localhost:8080",
                        "/uploads/images",
                        1024,
                        Set.of("image/jpeg", "image/png", "image/webp")
                )
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "menu.jpg",
                "image/jpeg",
                "image-bytes".getBytes()
        );

        UploadedImageResponse response = service.storeImage(file, UploadImageTarget.PLACE);

        assertThat(response.imageUrl()).startsWith("http://localhost:8080/uploads/images/places/");
        assertThat(response.originalFilename()).isEqualTo("menu.jpg");
        assertThat(response.contentType()).isEqualTo("image/jpeg");
        assertThat(response.size()).isEqualTo(file.getSize());

        String storedFileName = response.imageUrl().substring(response.imageUrl().lastIndexOf('/') + 1);
        assertThat(Files.readString(tempDir.resolve("places").resolve(storedFileName))).isEqualTo("image-bytes");
    }

    @Test
    void storeImage_rejectsNonImageFile() {
        ImageUploadService service = new ImageUploadService(
                new ImageUploadProperties(
                        tempDir,
                        "http://localhost:8080",
                        "/uploads/images",
                        1024,
                        Set.of("image/jpeg", "image/png", "image/webp")
                )
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "memo.txt",
                "text/plain",
                "not-image".getBytes()
        );

        assertThatThrownBy(() -> service.storeImage(file, UploadImageTarget.PROFILE))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
