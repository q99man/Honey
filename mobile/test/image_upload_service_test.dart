import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/api/api_client.dart';
import 'package:honeytong_mobile/core/api/api_endpoints.dart';
import 'package:honeytong_mobile/core/services/image_upload_service.dart';

void main() {
  test('uploads image file as multipart form data', () async {
    final adapter = _RecordingAdapter();
    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = adapter;
    adapter.respondWith({
      'success': true,
      'data': {
        'imageUrl': 'http://localhost:8080/uploads/images/places/menu.jpg',
      },
    });

    final service = ImageUploadService(apiClient);
    final result = await service.uploadImageBytes(
      bytes: Uint8List.fromList([1, 2, 3]),
      filename: 'menu.jpg',
      target: ImageUploadTarget.place,
    );

    expect(result, 'http://localhost:8080/uploads/images/places/menu.jpg');
    expect(adapter.requests.single.method, 'POST');
    expect(adapter.requests.single.path, ApiEndpoints.imageUploads);

    final formData = adapter.requests.single.data as FormData;
    expect(
      formData.fields,
      contains(
        isA<MapEntry<String, String>>()
            .having((entry) => entry.key, 'key', 'target')
            .having((entry) => entry.value, 'value', 'PLACE'),
      ),
    );
    expect(formData.files.single.key, 'file');
  });
}

class _RecordedRequest {
  const _RecordedRequest({
    required this.method,
    required this.path,
    required this.data,
  });

  final String method;
  final String path;
  final dynamic data;
}

class _RecordingAdapter implements HttpClientAdapter {
  final List<_RecordedRequest> requests = [];
  Map<String, dynamic> _body = {'success': true, 'data': null};
  int _statusCode = 200;

  void respondWith(Map<String, dynamic> body, {int statusCode = 200}) {
    _body = body;
    _statusCode = statusCode;
  }

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(
      _RecordedRequest(
        method: options.method,
        path: options.path,
        data: options.data,
      ),
    );

    return ResponseBody.fromString(
      jsonEncode(_body),
      _statusCode,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
