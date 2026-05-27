import 'dart:typed_data';

import 'package:dio/dio.dart';

import '../api/api_client.dart';
import '../api/api_endpoints.dart';

enum ImageUploadTarget {
  place('PLACE'),
  profile('PROFILE'),
  visit('VISIT');

  const ImageUploadTarget(this.apiValue);

  final String apiValue;
}

class ImageUploadService {
  ImageUploadService(this._apiClient);

  final ApiClient _apiClient;

  Future<String?> uploadImageFile({
    required String path,
    required String filename,
    required ImageUploadTarget target,
  }) async {
    final multipartFile = await MultipartFile.fromFile(
      path,
      filename: filename,
    );
    return _uploadMultipartFile(
      multipartFile: multipartFile,
      target: target,
    );
  }

  Future<String?> uploadImageBytes({
    required Uint8List bytes,
    required String filename,
    required ImageUploadTarget target,
  }) async {
    final multipartFile = MultipartFile.fromBytes(
      bytes,
      filename: filename,
    );
    return _uploadMultipartFile(
      multipartFile: multipartFile,
      target: target,
    );
  }

  Future<String?> _uploadMultipartFile({
    required MultipartFile multipartFile,
    required ImageUploadTarget target,
  }) async {
    try {
      final formData = FormData.fromMap({
        'target': target.apiValue,
        'file': multipartFile,
      });

      final response = await _apiClient.dio.post(
        ApiEndpoints.imageUploads,
        data: formData,
      );

      if (response.statusCode == 200 && response.data != null) {
        final data = response.data['data'];
        if (data is Map && data['imageUrl'] is String) {
          return data['imageUrl'] as String;
        }
      }
      return null;
    } catch (_) {
      return null;
    }
  }
}
