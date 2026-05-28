import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/config/app_config.dart';

void main() {
  group('AppConfig', () {
    test('disables Kakao native features when native key is not provided', () {
      expect(AppConfig.kakaoNativeAppKey, isEmpty);
      expect(AppConfig.isKakaoNativeConfigured, isFalse);
    });

    test('provides a display-only map center without treating it as GPS', () {
      expect(AppConfig.defaultMapLatitude, isA<double>());
      expect(AppConfig.defaultMapLongitude, isA<double>());
    });
  });
}
