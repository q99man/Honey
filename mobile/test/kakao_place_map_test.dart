import 'package:flutter_test/flutter_test.dart';
import 'package:geolocator/geolocator.dart';
import 'package:honeytong_mobile/features/home/widgets/kakao_place_map.dart';
import 'package:honeytong_mobile/models/place.dart';

void main() {
  test('best center follows the selected place instead of the first marker',
      () {
    final firstPlace = _place(
      id: 1,
      name: '비에뜨반미',
      latitude: 37.5010,
      longitude: 127.0396,
    );
    final selectedPlace = _place(
      id: 2,
      name: '대동식당',
      latitude: 37.5025,
      longitude: 127.0410,
    );

    final center = KakaoPlaceMap.bestCenterForTesting(
      places: [firstPlace, selectedPlace],
      selectedPlaceId: selectedPlace.id,
      currentPosition: null,
      preferCurrentPosition: false,
    );

    expect(center.latitude, selectedPlace.latitude);
    expect(center.longitude, selectedPlace.longitude);
  });

  test('best center still follows current position for explicit recenter', () {
    final selectedPlace = _place(
      id: 2,
      name: '대동식당',
      latitude: 37.5025,
      longitude: 127.0410,
    );
    final currentPosition = _position(latitude: 37.5000, longitude: 127.0300);

    final center = KakaoPlaceMap.bestCenterForTesting(
      places: [selectedPlace],
      selectedPlaceId: selectedPlace.id,
      currentPosition: currentPosition,
      preferCurrentPosition: true,
    );

    expect(center.latitude, currentPosition.latitude);
    expect(center.longitude, currentPosition.longitude);
  });

  test('closing the selected card does not move the camera', () {
    final shouldMoveCamera = KakaoPlaceMap.shouldMoveCameraForTesting(
      placesChanged: false,
      recenterRequested: false,
      positionChanged: false,
      oldSelectedPlaceId: 2,
      newSelectedPlaceId: null,
    );

    expect(shouldMoveCamera, isFalse);
  });
}

Place _place({
  required int id,
  required String name,
  required double latitude,
  required double longitude,
}) {
  return Place(
    id: id,
    name: name,
    categoryCode: 'KOREAN',
    recommendedMenu: '추천 메뉴',
    shortRecommendation: '추천 문구',
    addressRoad: '도로명 주소',
    addressJibun: '지번 주소',
    latitude: latitude,
    longitude: longitude,
    imageUrls: const [],
    currentStarLevel: 0,
  );
}

Position _position({
  required double latitude,
  required double longitude,
}) {
  return Position(
    latitude: latitude,
    longitude: longitude,
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
