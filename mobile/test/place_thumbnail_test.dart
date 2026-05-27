import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/theme/app_theme.dart';
import 'package:honeytong_mobile/features/place/widgets/place_thumbnail.dart';

void main() {
  testWidgets('PlaceThumbnail renders image when url is available',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: const Scaffold(
          body: PlaceThumbnail(
            imageUrl: 'https://example.com/place.jpg',
            size: 64,
          ),
        ),
      ),
    );

    expect(find.byType(Image), findsOneWidget);
    expect(find.byIcon(Icons.restaurant_rounded), findsNothing);
  });

  testWidgets('PlaceThumbnail renders fallback icon without image url',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: const Scaffold(
          body: PlaceThumbnail(
            imageUrl: null,
            size: 64,
          ),
        ),
      ),
    );

    expect(find.byIcon(Icons.restaurant_rounded), findsOneWidget);
  });
}
