import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/models/user.dart';

void main() {
  group('User model tests', () {
    test('parses backend user profile response without obsolete username', () {
      final profile = UserProfile.fromJson({
        'id': 28,
        'nickname': '허니테스터',
        'phoneVerified': true,
        'languagePreference': 'ko',
        'birthYear': 1990,
        'gender': 'UNKNOWN',
        'nationalityCode': 'KR',
        'profileImageUrl':
            'http://localhost:8080/uploads/images/profiles/profile.jpg',
      });

      expect(profile.id, 28);
      expect(profile.nickname, '허니테스터');
      expect(profile.displayName, '허니테스터');
      expect(profile.phoneVerified, isTrue);
      expect(profile.languagePreference, 'ko');
      expect(profile.profileImageUrl,
          'http://localhost:8080/uploads/images/profiles/profile.jpg');
    });

    test('parses backend activity summary field names', () {
      final summary = UserActivitySummary.fromJson({
        'recommendedCount': 3,
        'visitCount': 2,
        'commentCount': 1,
        'registeredPlaceCount': 4,
      });

      expect(summary.recommendationCount, 3);
      expect(summary.visitCount, 2);
      expect(summary.commentCount, 1);
      expect(summary.placeCount, 4);
    });
  });
}
