import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/theme/app_theme.dart';
import 'package:honeytong_mobile/features/place/widgets/place_image_gallery.dart';

void main() {
  testWidgets('PlaceImageGallery exposes image count and thumbnail navigation',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: const Scaffold(
          body: SizedBox(
            width: 390,
            height: 260,
            child: PlaceImageGallery(
              imageUrls: [
                'https://example.com/first.jpg',
                'https://example.com/second.jpg',
              ],
            ),
          ),
        ),
      ),
    );

    expect(find.text('1 / 2'), findsOneWidget);
    expect(find.byTooltip('2번 사진 보기'), findsOneWidget);

    await tester.tap(find.byTooltip('2번 사진 보기'));
    await tester.pumpAndSettle();

    expect(find.text('2 / 2'), findsOneWidget);
  });
}
