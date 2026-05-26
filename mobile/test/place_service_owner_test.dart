import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/api/api_client.dart';
import 'package:honeytong_mobile/core/api/api_endpoints.dart';
import 'package:honeytong_mobile/features/place/services/place_service.dart';

void main() {
  group('PlaceService owner place APIs', () {
    late _RecordingAdapter adapter;
    late PlaceService service;

    setUp(() {
      adapter = _RecordingAdapter();
      final apiClient = ApiClient();
      apiClient.dio.interceptors.clear();
      apiClient.dio.httpClientAdapter = adapter;
      service = PlaceService(apiClient);
    });

    test('loads my registered places from the owner list endpoint', () async {
      adapter.respondWith({
        'success': true,
        'data': [
          {
            'id': 1001,
            'name': '부평 국밥',
            'categoryCode': 'KOREAN',
            'regionName': '부평1동',
            'address': '인천 부평구 경원대로 1',
            'latitude': 37.491013,
            'longitude': 126.720600,
            'shortRecommendation': '동네에서 다시 찾기 좋은 한 끼',
            'starLevel': 1,
            'recommendCount': 3,
            'visitCount': 2,
            'commentCount': 1,
          },
        ],
      });

      final places = await service.getMyRegisteredPlaces();

      expect(adapter.requests.single.method, 'GET');
      expect(adapter.requests.single.path, ApiEndpoints.userPlaces);
      expect(places, hasLength(1));
      expect(places.single.name, '부평 국밥');
      expect(places.single.addressRoad, '인천 부평구 경원대로 1');
      expect(places.single.stats?.recommendCount, 3);
    });

    test('updates a place through the owner edit endpoint', () async {
      adapter.respondWith({
        'success': true,
        'data': {'placeId': 1001},
        'message': '맛집 정보가 수정되었습니다.',
      });

      final result = await service.updatePlace(1001, {
        'name': '부평 국밥 수정',
        'addressRoad': '인천 부평구 경원대로 2',
      });

      expect(adapter.requests.single.method, 'PATCH');
      expect(adapter.requests.single.path, ApiEndpoints.placeDetail(1001));
      expect(adapter.requests.single.data['name'], '부평 국밥 수정');
      expect(result['success'], isTrue);
      expect(result['message'], '맛집 정보가 수정되었습니다.');
    });

    test('deletes a place through the owner delete endpoint', () async {
      adapter.respondWith({
        'success': true,
        'data': {'placeId': 1001, 'deleted': true},
        'message': '맛집이 삭제되었습니다.',
      });

      final result = await service.deletePlace(1001);

      expect(adapter.requests.single.method, 'DELETE');
      expect(adapter.requests.single.path, ApiEndpoints.placeDetail(1001));
      expect(result['success'], isTrue);
      expect(result['data']['deleted'], isTrue);
    });
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
