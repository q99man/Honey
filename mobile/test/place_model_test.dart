import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/models/place.dart';

void main() {
  group('Place Model Tests', () {
    test('parses backend list item response fields', () {
      final place = Place.fromJson({
        'id': 1001,
        'name': '상동 순대국',
        'categoryCode': 'KOREAN',
        'regionName': '상동',
        'address': '경기도 부천시 원미구 상동로 117',
        'latitude': 37.503456,
        'longitude': 126.753456,
        'shortRecommendation': '진한 육수와 푸짐한 건더기가 일품입니다.',
        'starLevel': 2,
        'recommendCount': 12,
        'visitCount': 4,
        'commentCount': 3,
        'representativeImageUrl': 'https://example.com/place.jpg',
      });

      expect(place.id, 1001);
      expect(place.name, '상동 순대국');
      expect(place.regionDongName, '상동');
      expect(place.addressRoad, '경기도 부천시 원미구 상동로 117');
      expect(place.currentStarLevel, 2);
      expect(place.imageUrls, ['https://example.com/place.jpg']);
      expect(place.stats?.recommendCount, 12);
      expect(place.stats?.visitCount, 4);
      expect(place.stats?.commentCount, 3);
    });

    test('parses backend detail response fields', () {
      final place = Place.fromJson({
        'id': 1002,
        'name': '홍대 츠케멘',
        'categoryCode': 'JAPANESE',
        'dongName': '서교동',
        'addressRoad': '서울특별시 마포구 와우산로 23길 9',
        'addressJibun': '서교동 345-12',
        'latitude': 37.556456,
        'longitude': 126.924456,
        'recommendedMenu': '매운 츠케멘',
        'shortRecommendation': '면발이 좋은 골목 맛집입니다.',
        'starLevel': 1,
        'recommendCount': 7,
        'visitCount': 5,
        'commentCount': 2,
        'imageUrls': ['https://example.com/detail.jpg'],
      });

      expect(place.regionDongName, '서교동');
      expect(place.addressRoad, '서울특별시 마포구 와우산로 23길 9');
      expect(place.addressJibun, '서교동 345-12');
      expect(place.recommendedMenu, '매운 츠케멘');
      expect(place.currentStarLevel, 1);
      expect(place.imageUrls, ['https://example.com/detail.jpg']);
      expect(place.stats?.recommendCount, 7);
    });
  });
}
