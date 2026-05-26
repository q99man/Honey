import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/features/place/models/place_detail_address.dart';

void main() {
  group('PlaceDetailAddress', () {
    test('does not render jibun line when jibun address is null', () {
      final address = PlaceDetailAddress.fromJson({
        'addressRoad': '인천 부평구 경원대로1377번길 47',
        'addressJibun': null,
      });

      expect(address.road, '인천 부평구 경원대로1377번길 47');
      expect(address.jibunLabel, isNull);
    });

    test('does not render jibun line when backend sends literal null text', () {
      final address = PlaceDetailAddress.fromJson({
        'addressRoad': '인천 부평구 경원대로1377번길 47',
        'addressJibun': 'null',
      });

      expect(address.jibunLabel, isNull);
    });

    test('renders jibun line when jibun address exists', () {
      final address = PlaceDetailAddress.fromJson({
        'addressRoad': '인천 부평구 경원대로1377번길 47',
        'addressJibun': '인천 부평구 부평동 546-98',
      });

      expect(address.jibunLabel, '[지번] 인천 부평구 부평동 546-98');
    });
  });
}
