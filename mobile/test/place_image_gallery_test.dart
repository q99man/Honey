import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/theme/app_theme.dart';
import 'package:honeytong_mobile/features/place/widgets/place_image_gallery.dart';

void main() {
  Future<void> pumpGallery(WidgetTester tester) async {
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
  }

  testWidgets('PlaceImageGallery exposes image count and thumbnail navigation',
      (tester) async {
    await pumpGallery(tester);

    expect(find.text('1 / 2'), findsOneWidget);
    expect(find.byTooltip('2번째 사진 보기'), findsOneWidget);

    await tester.tap(find.byTooltip('2번째 사진 보기'));
    await tester.pumpAndSettle();

    expect(find.text('2 / 2'), findsOneWidget);
  });

  testWidgets('PlaceImageGallery changes image from horizontal drag',
      (tester) async {
    await pumpGallery(tester);

    expect(find.text('1 / 2'), findsOneWidget);

    await tester.drag(find.byType(PlaceImageGallery), const Offset(-260, 0));
    await tester.pumpAndSettle();

    expect(find.text('2 / 2'), findsOneWidget);
  });

  testWidgets('PlaceImageGallery opens a dismissible fullscreen image viewer',
      (tester) async {
    await pumpGallery(tester);

    await tester.tap(find.byType(PlaceImageGallery));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('place-image-viewer')), findsOneWidget);
    expect(find.byTooltip('사진 닫기'), findsOneWidget);
    expect(find.text('1 / 2'), findsNWidgets(2));

    await tester.tap(find.byTooltip('사진 닫기'));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('place-image-viewer')), findsNothing);
  });

  testWidgets('PlaceImageGallery fullscreen viewer starts from current image',
      (tester) async {
    await pumpGallery(tester);

    await tester.tap(find.byTooltip('2번째 사진 보기'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(PlaceImageGallery));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('place-image-viewer')), findsOneWidget);
    expect(find.text('2 / 2'), findsNWidgets(2));
  });

  testWidgets('PlaceImageGallery fullscreen viewer changes image from drag',
      (tester) async {
    await pumpGallery(tester);

    await tester.tap(find.byType(PlaceImageGallery));
    await tester.pumpAndSettle();
    await tester.drag(
      find.byKey(const Key('place-image-viewer-page-view')),
      const Offset(-260, 0),
    );
    await tester.pumpAndSettle();

    expect(
      find.descendant(
        of: find.byKey(const Key('place-image-viewer')),
        matching: find.text('2 / 2'),
      ),
      findsOneWidget,
    );
  });
}
