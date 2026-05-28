import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/features/home/widgets/home_map_marker_asset.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  test('HomeMapMarkerAsset creates branded PNG bytes for place markers',
      () async {
    final koreanMarker = await HomeMapMarkerAsset.place(categoryCode: 'KOREAN');
    final cafeMarker = await HomeMapMarkerAsset.place(categoryCode: 'CAFE');

    expect(koreanMarker.length, greaterThan(500));
    expect(koreanMarker.take(8), [137, 80, 78, 71, 13, 10, 26, 10]);
    expect(cafeMarker.length, greaterThan(500));
    expect(cafeMarker, isNot(koreanMarker));
  });

  test('HomeMapMarkerAsset creates a stronger selected place marker', () async {
    final normalMarker = await HomeMapMarkerAsset.place(categoryCode: 'KOREAN');
    final selectedMarker = await HomeMapMarkerAsset.place(
      categoryCode: 'KOREAN',
      selected: true,
    );

    expect(selectedMarker.length, greaterThan(500));
    expect(selectedMarker.take(8), [137, 80, 78, 71, 13, 10, 26, 10]);
    expect(selectedMarker, isNot(normalMarker));
    expect(selectedMarker.length, greaterThan(normalMarker.length));
  });

  test('HomeMapMarkerAsset creates a distinct current location marker',
      () async {
    final placeMarker = await HomeMapMarkerAsset.place(categoryCode: 'KOREAN');
    final currentMarker = await HomeMapMarkerAsset.currentLocation();

    expect(currentMarker.length, greaterThan(500));
    expect(currentMarker.take(8), [137, 80, 78, 71, 13, 10, 26, 10]);
    expect(currentMarker, isNot(placeMarker));
  });
}
