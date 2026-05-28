import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/api/api_client.dart';
import 'package:honeytong_mobile/core/api/api_endpoints.dart';
import 'package:honeytong_mobile/features/auth/services/auth_service.dart';

void main() {
  test('login returns backend error message when credentials are rejected',
      () async {
    final adapter = _AuthAdapter();
    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = adapter;
    adapter.respondWith(
      {
        'success': false,
        'errorCode': 'INVALID_CREDENTIALS',
        'message': '이메일 또는 비밀번호가 올바르지 않습니다.',
      },
      statusCode: 401,
    );

    final service = AuthService(apiClient);
    final result = await service.login('wrong@example.com', 'bad-password');

    expect(result.success, isFalse);
    expect(result.errorMessage, '이메일 또는 비밀번호가 올바르지 않습니다.');
    expect(adapter.requests.single.method, 'POST');
    expect(adapter.requests.single.path, ApiEndpoints.login);
  });

  test('signup returns backend validation message', () async {
    final adapter = _AuthAdapter();
    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = adapter;
    adapter.respondWith(
      {
        'success': false,
        'errorCode': 'DUPLICATE_EMAIL',
        'message': '이미 사용 중인 이메일입니다.',
      },
      statusCode: 400,
    );

    final service = AuthService(apiClient);
    final result = await service.signup(
      'used@example.com',
      'password123',
      '허니',
      '01012345678',
    );

    expect(result.success, isFalse);
    expect(result.errorMessage, '이미 사용 중인 이메일입니다.');
    expect(adapter.requests.single.method, 'POST');
    expect(adapter.requests.single.path, ApiEndpoints.signup);
  });

  test('login returns Korean connection message when backend is unreachable',
      () async {
    final adapter = _AuthAdapter()..failWithConnectionError = true;
    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = adapter;

    final service = AuthService(apiClient);
    final result = await service.login('user@example.com', 'password123');

    expect(result.success, isFalse);
    expect(
      result.errorMessage,
      '서버에 연결하지 못했습니다. 네트워크와 개발 서버 연결을 확인해주세요.',
    );
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

class _AuthAdapter implements HttpClientAdapter {
  final List<_RecordedRequest> requests = [];
  Map<String, dynamic> _body = {'success': true, 'data': null};
  int _statusCode = 200;
  bool failWithConnectionError = false;

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

    if (failWithConnectionError) {
      throw DioException.connectionError(
        requestOptions: options,
        reason: 'connection refused',
      );
    }

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
