package com.honeytong.common.upload;

import com.honeytong.common.api.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ImageUploadController {

    private final ImageUploadService imageUploadService;

    public ImageUploadController(ImageUploadService imageUploadService) {
        this.imageUploadService = imageUploadService;
    }

    @PostMapping(value = "/api/uploads/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadedImageResponse> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "PLACE") UploadImageTarget target
    ) {
        return ApiResponse.success(imageUploadService.storeImage(file, target), "이미지가 업로드되었습니다.");
    }
}
