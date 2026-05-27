import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/theme/app_theme.dart';
import 'package:honeytong_mobile/features/home/widgets/home_place_card.dart';
import 'package:honeytong_mobile/models/place.dart';

void main() {
  testWidgets('HomePlaceCard renders a place summary in Korean',
      (tester) async {
    final place = Place.fromJson({
      'id': 1,
      'name': '상동 순대국',
      'categoryCode': 'KOREAN',
      'address': '경기도 부천시 상동로',
      'recommendedMenu': '순대국',
      'shortRecommendation': '진한 국물이 좋은 동네 맛집입니다.',
      'recommendCount': 12,
      'visitCount': 4,
      'commentCount': 2,
    });

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: HomePlaceCard(
            place: place,
            categoryLabel: '한식',
            onTap: () {},
          ),
        ),
      ),
    );

    expect(find.text('상동 순대국'), findsOneWidget);
    expect(find.text('한식'), findsOneWidget);
    expect(find.text('추천 메뉴: 순대국'), findsOneWidget);
    expect(find.text('경기도 부천시 상동로'), findsOneWidget);
    expect(find.byIcon(Icons.restaurant_rounded), findsOneWidget);
  });

  testWidgets('HomePlaceCard uses representative image when available',
      (tester) async {
    final place = Place.fromJson({
      'id': 1,
      'name': '상동 순대국',
      'categoryCode': 'KOREAN',
      'address': '경기도 부천시 상동로',
      'recommendedMenu': '순대국',
      'shortRecommendation': '진한 국물이 좋은 동네 맛집입니다.',
      'representativeImageUrl': 'https://example.com/place.jpg',
    });

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: HomePlaceCard(
            place: place,
            categoryLabel: '한식',
            onTap: () {},
          ),
        ),
      ),
    );

    expect(find.byType(Image), findsOneWidget);
    expect(find.byIcon(Icons.restaurant_rounded), findsNothing);
  });
}
