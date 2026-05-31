import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:geolocator_platform_interface/geolocator_platform_interface.dart';
import 'package:honeytong_mobile/core/api/api_client.dart';
import 'package:honeytong_mobile/features/home/views/home_map_screen.dart';
import 'package:honeytong_mobile/models/place.dart';
import 'package:honeytong_mobile/utils/localization.dart';
import 'package:provider/provider.dart';

void main() {
  test('home map uses Korean-first translation keys for visible UI copy', () {
    expect('home.searchHint'.tr, '맛집 이름이나 메뉴를 검색해 보세요');
    expect('home.category.all'.tr, '전체');
    expect('home.category.korean'.tr, '한식');
    expect('home.registerPlace'.tr, '맛집 등록');
    expect('home.currentLocation'.tr, '내 위치');
    expect('home.mapConfigurationTitle'.tr, '카카오맵 설정이 필요합니다');
    expect('home.locatingTitle'.tr, '현재 위치 확인 중');
    expect('home.loadingNearbyTitle'.tr, '주변 맛집 불러오는 중');
    expect('home.permissionEmptyTitle'.tr, '위치 권한이 필요합니다');
    expect('home.nearbyEmptyTitle'.tr, '주변 맛집이 아직 없습니다');
    expect('home.filterEmptyTitle'.tr, '선택한 필터에 맞는 맛집이 없습니다');
  });

  testWidgets('HomeMapScreen renders Korean real-device map controls',
      (tester) async {
    final originalGeolocator = GeolocatorPlatform.instance;
    GeolocatorPlatform.instance = _FakeGeolocatorPlatform(
      permission: LocationPermission.deniedForever,
      position: _testPosition(),
    );
    addTearDown(() => GeolocatorPlatform.instance = originalGeolocator);

    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = _EmptyPlacesAdapter();

    await tester.pumpWidget(
      Provider<ApiClient>.value(
        value: apiClient,
        child: const MaterialApp(home: HomeMapScreen()),
      ),
    );
    await tester.pump();
    await tester.pumpAndSettle();

    expect(find.text('전체'), findsOneWidget);
    expect(find.text('한식'), findsOneWidget);
    expect(find.byTooltip('맛집 등록'), findsOneWidget);
    expect(find.byTooltip('내 위치'), findsOneWidget);
    expect(find.text('카카오맵 설정이 필요합니다'), findsOneWidget);
  });

  testWidgets('HomeMapScreen animates map overlay transitions', (tester) async {
    final originalGeolocator = GeolocatorPlatform.instance;
    GeolocatorPlatform.instance = _FakeGeolocatorPlatform(
      permission: LocationPermission.deniedForever,
      position: _testPosition(),
    );
    addTearDown(() => GeolocatorPlatform.instance = originalGeolocator);

    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = _EmptyPlacesAdapter();

    await tester.pumpWidget(
      Provider<ApiClient>.value(
        value: apiClient,
        child: const MaterialApp(home: HomeMapScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.byKey(const ValueKey('home-map-floating-actions')),
      findsOneWidget,
    );
    expect(
      find.byKey(const ValueKey('home-map-selected-card-switcher')),
      findsOneWidget,
    );
  });

  testWidgets('HomeMapScreen does not query nearby places before GPS succeeds',
      (tester) async {
    final originalGeolocator = GeolocatorPlatform.instance;
    GeolocatorPlatform.instance = _FakeGeolocatorPlatform(
      permission: LocationPermission.deniedForever,
      position: _testPosition(),
    );
    addTearDown(() => GeolocatorPlatform.instance = originalGeolocator);

    final adapter = _EmptyPlacesAdapter();
    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = adapter;

    await tester.pumpWidget(
      Provider<ApiClient>.value(
        value: apiClient,
        child: const MaterialApp(home: HomeMapScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(adapter.requestPaths, isEmpty);
  });

  testWidgets('HomeMapScreen separates location permission empty state',
      (tester) async {
    final originalGeolocator = GeolocatorPlatform.instance;
    GeolocatorPlatform.instance = _FakeGeolocatorPlatform(
      permission: LocationPermission.deniedForever,
      position: _testPosition(),
    );
    addTearDown(() => GeolocatorPlatform.instance = originalGeolocator);

    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = _EmptyPlacesAdapter();

    await tester.pumpWidget(
      Provider<ApiClient>.value(
        value: apiClient,
        child: const MaterialApp(home: HomeMapScreen()),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('리스트 보기'));
    await tester.pumpAndSettle();

    expect(find.text('위치 권한이 필요합니다'), findsOneWidget);
  });

  testWidgets('HomeMapScreen loads nearby places from GPS on first entry',
      (tester) async {
    final originalGeolocator = GeolocatorPlatform.instance;
    final geolocator = _FakeGeolocatorPlatform(
      permission: LocationPermission.whileInUse,
      position: _testPosition(),
    );
    GeolocatorPlatform.instance = geolocator;
    addTearDown(() => GeolocatorPlatform.instance = originalGeolocator);

    final adapter = _EmptyPlacesAdapter();
    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = adapter;

    await tester.pumpWidget(
      Provider<ApiClient>.value(
        value: apiClient,
        child: const MaterialApp(home: HomeMapScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(geolocator.checkPermissionCalls, 1);
    expect(geolocator.requestPermissionCalls, 0);
    expect(geolocator.getCurrentPositionCalls, 1);
    expect(adapter.requestPaths, contains('/api/places/nearby'));
  });

  testWidgets('HomeMapScreen separates nearby empty state after GPS succeeds',
      (tester) async {
    final originalGeolocator = GeolocatorPlatform.instance;
    GeolocatorPlatform.instance = _FakeGeolocatorPlatform(
      permission: LocationPermission.whileInUse,
      position: _testPosition(),
    );
    addTearDown(() => GeolocatorPlatform.instance = originalGeolocator);

    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = _EmptyPlacesAdapter();

    await tester.pumpWidget(
      Provider<ApiClient>.value(
        value: apiClient,
        child: const MaterialApp(home: HomeMapScreen()),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('리스트 보기'));
    await tester.pumpAndSettle();

    expect(find.text('주변 맛집이 아직 없습니다'), findsOneWidget);
  });

  testWidgets('HomeMapScreen separates category filter empty state',
      (tester) async {
    final originalGeolocator = GeolocatorPlatform.instance;
    GeolocatorPlatform.instance = _FakeGeolocatorPlatform(
      permission: LocationPermission.whileInUse,
      position: _testPosition(),
    );
    addTearDown(() => GeolocatorPlatform.instance = originalGeolocator);

    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = _EmptyPlacesAdapter(data: [
      {
        'id': 1,
        'name': '비에뜨반미',
        'categoryCode': 'WESTERN',
        'recommendedMenu': '반미',
        'shortRecommendation': '바삭한 반미 맛집',
        'addressRoad': '경기 부천시 평천로',
        'latitude': 37.5010,
        'longitude': 127.0396,
      }
    ]);

    await tester.pumpWidget(
      Provider<ApiClient>.value(
        value: apiClient,
        child: const MaterialApp(home: HomeMapScreen()),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('한식'));
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('리스트 보기'));
    await tester.pumpAndSettle();

    expect(find.text('선택한 필터에 맞는 맛집이 없습니다'), findsOneWidget);
  });

  testWidgets('HomeMapScreen asks for location permission on first entry',
      (tester) async {
    final originalGeolocator = GeolocatorPlatform.instance;
    final geolocator = _FakeGeolocatorPlatform(
      permission: LocationPermission.denied,
      requestedPermission: LocationPermission.whileInUse,
      position: _testPosition(),
    );
    GeolocatorPlatform.instance = geolocator;
    addTearDown(() => GeolocatorPlatform.instance = originalGeolocator);

    final apiClient = ApiClient();
    apiClient.dio.interceptors.clear();
    apiClient.dio.httpClientAdapter = _EmptyPlacesAdapter();

    await tester.pumpWidget(
      Provider<ApiClient>.value(
        value: apiClient,
        child: const MaterialApp(home: HomeMapScreen()),
      ),
    );
    await tester.pumpAndSettle();

    expect(geolocator.checkPermissionCalls, 1);
    expect(geolocator.requestPermissionCalls, 1);
    expect(geolocator.getCurrentPositionCalls, 1);
  });

  test('HomeMapScreen keeps selected place fresh after nearby reload', () {
    final staleSelection = Place.fromJson({
      'id': 1,
      'name': '대동식당',
      'categoryCode': 'KOREAN',
      'recommendedMenu': '순대국',
      'shortRecommendation': '진한 국물이 좋은 동네 맛집입니다.',
      'latitude': 37.5010,
      'longitude': 127.0396,
    });
    final reloadedPlace = Place.fromJson({
      'id': 1,
      'name': '대동식당',
      'categoryCode': 'KOREAN',
      'recommendedMenu': '얼큰 순대국',
      'shortRecommendation': '방문 후 갱신된 추천 문구입니다.',
      'latitude': 37.5010,
      'longitude': 127.0396,
    });

    final synced = HomeMapScreen.syncSelectedPlaceForTesting(
      selectedPlace: staleSelection,
      visiblePlaces: [reloadedPlace],
    );

    expect(synced, same(reloadedPlace));
    expect(synced?.recommendedMenu, '얼큰 순대국');
  });

  test('HomeMapScreen preserves selected place when toggling view modes', () {
    final selectedPlace = Place.fromJson({
      'id': 1,
      'name': 'Daedong Sikdang',
      'categoryCode': 'KOREAN',
      'recommendedMenu': 'Soup',
      'shortRecommendation': 'A local favorite.',
      'latitude': 37.5010,
      'longitude': 127.0396,
    });

    final nextSelection = HomeMapScreen.selectedPlaceAfterViewToggleForTesting(
      selectedPlace: selectedPlace,
    );

    expect(nextSelection, same(selectedPlace));
  });
}

class _EmptyPlacesAdapter implements HttpClientAdapter {
  _EmptyPlacesAdapter({this.data = const []});

  final List<Map<String, dynamic>> data;
  final requestPaths = <String>[];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requestPaths.add(options.path);

    return ResponseBody.fromString(
      jsonEncode({'success': true, 'data': data}),
      200,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}

Position _testPosition() {
  return Position(
    latitude: 37.5010,
    longitude: 127.0396,
    timestamp: DateTime(2026, 5, 28),
    accuracy: 8,
    altitude: 0,
    altitudeAccuracy: 0,
    heading: 0,
    headingAccuracy: 0,
    speed: 0,
    speedAccuracy: 0,
  );
}

class _FakeGeolocatorPlatform extends GeolocatorPlatform {
  _FakeGeolocatorPlatform({
    required this.permission,
    required this.position,
    this.requestedPermission,
    this.serviceEnabled = true,
  });

  LocationPermission permission;
  final LocationPermission? requestedPermission;
  final Position position;
  final bool serviceEnabled;
  int checkPermissionCalls = 0;
  int requestPermissionCalls = 0;
  int getCurrentPositionCalls = 0;

  @override
  Future<bool> isLocationServiceEnabled() async => serviceEnabled;

  @override
  Future<LocationPermission> checkPermission() async {
    checkPermissionCalls += 1;
    return permission;
  }

  @override
  Future<LocationPermission> requestPermission() async {
    requestPermissionCalls += 1;
    permission = requestedPermission ?? permission;
    return permission;
  }

  @override
  Future<Position> getCurrentPosition({
    LocationSettings? locationSettings,
  }) async {
    getCurrentPositionCalls += 1;
    return position;
  }
}
