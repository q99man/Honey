import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/features/place/utils/place_category.dart';

void main() {
  test('PlaceCategory provides Korean-first labels from one shared source', () {
    expect(PlaceCategory.labelFor('KOREAN'), '한식');
    expect(PlaceCategory.labelFor('CAFE'), '카페');
    expect(PlaceCategory.labelFor('UNKNOWN'), 'UNKNOWN');
  });

  test('PlaceCategory exposes stable selectable category order', () {
    expect(
      PlaceCategory.selectableCodes,
      ['KOREAN', 'CHINESE', 'JAPANESE', 'WESTERN', 'SNACK', 'CAFE'],
    );
    expect(PlaceCategory.contains('WESTERN'), isTrue);
    expect(PlaceCategory.contains('UNKNOWN'), isFalse);
  });

  test('PlaceCategory provides marker style identity per category', () {
    expect(PlaceCategory.markerStyleIdFor('KOREAN'), 'honey_place_KOREAN');
    expect(
      PlaceCategory.markerStyleIdFor('KOREAN', selected: true),
      'honey_place_KOREAN_selected',
    );
    expect(PlaceCategory.markerStyleIdFor('UNKNOWN'), 'honey_place_default');
  });

  test('PlaceCategory reuses the legacy web category emoji mapping', () {
    expect(PlaceCategory.emojiFor('KOREAN'), '🍚');
    expect(PlaceCategory.emojiFor('CHINESE'), '🥢');
    expect(PlaceCategory.emojiFor('JAPANESE'), '🍣');
    expect(PlaceCategory.emojiFor('WESTERN'), '🍝');
    expect(PlaceCategory.emojiFor('SNACK'), '🍢');
    expect(PlaceCategory.emojiFor('CAFE'), '☕');
  });
}
