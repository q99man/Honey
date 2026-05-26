import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/features/place/models/place_registration_eligibility.dart';

void main() {
  group('PlaceRegistrationEligibility', () {
    test('requires phone verification before place registration', () {
      final result = PlaceRegistrationEligibility.evaluate(
        phoneVerified: false,
        hasVerifiedRegion: true,
      );

      expect(result.allowed, isFalse);
      expect(result.message, '맛집 등록은 휴대폰 인증을 완료한 뒤 이용할 수 있습니다.');
    });

    test('requires region verification before place registration', () {
      final result = PlaceRegistrationEligibility.evaluate(
        phoneVerified: true,
        hasVerifiedRegion: false,
      );

      expect(result.allowed, isFalse);
      expect(result.message, '맛집 등록은 동네 인증을 완료한 뒤 이용할 수 있습니다.');
    });

    test('allows registration after phone and region verification', () {
      final result = PlaceRegistrationEligibility.evaluate(
        phoneVerified: true,
        hasVerifiedRegion: true,
      );

      expect(result.allowed, isTrue);
      expect(result.message, isNull);
    });
  });
}
