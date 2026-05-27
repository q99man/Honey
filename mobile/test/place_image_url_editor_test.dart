import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/theme/app_theme.dart';
import 'package:honeytong_mobile/features/place/widgets/place_image_url_editor.dart';

void main() {
  testWidgets('renders empty image URL guide and add action', (tester) async {
    final controller = TextEditingController();
    var addCount = 0;

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: PlaceImageUrlEditor(
            imageUrls: const [],
            controller: controller,
            onAdd: () => addCount++,
            onRemove: (_) {},
            onMoveUp: (_) {},
            onMoveDown: (_) {},
          ),
        ),
      ),
    );

    expect(find.text('맛집 사진'), findsOneWidget);
    expect(find.text('사진 URL'), findsOneWidget);
    expect(find.text('사진 URL을 추가하면 상세 화면과 저장 목록에 표시됩니다.'), findsOneWidget);

    await tester.tap(find.byTooltip('사진 추가'));
    expect(addCount, 1);

    controller.dispose();
  });

  testWidgets('renders image list controls', (tester) async {
    final controller = TextEditingController();
    final removed = <int>[];
    final movedUp = <int>[];
    final movedDown = <int>[];

    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: PlaceImageUrlEditor(
            imageUrls: const [
              'https://example.com/first.jpg',
              'https://example.com/second.jpg',
            ],
            controller: controller,
            onAdd: () {},
            onRemove: removed.add,
            onMoveUp: movedUp.add,
            onMoveDown: movedDown.add,
          ),
        ),
      ),
    );

    expect(find.text('1번 사진'), findsOneWidget);
    expect(find.text('2번 사진'), findsOneWidget);
    expect(find.text('대표'), findsOneWidget);

    await tester.tap(find.byTooltip('아래로 이동').first);
    await tester.tap(find.byTooltip('위로 이동').last);
    await tester.tap(find.byTooltip('삭제').first);

    expect(movedDown, [0]);
    expect(movedUp, [1]);
    expect(removed, [0]);

    controller.dispose();
  });
}
