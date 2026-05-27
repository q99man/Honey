import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/theme/app_theme.dart';
import 'package:honeytong_mobile/core/widgets/app_empty_state.dart';

void main() {
  testWidgets('AppEmptyState renders Korean title, description, and action',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: AppEmptyState(
            icon: Icons.map_outlined,
            title: '주변 맛집이 없습니다',
            description: '검색어나 위치를 바꿔 다시 탐색해보세요.',
            actionLabel: '다시 찾기',
            onActionPressed: () {},
          ),
        ),
      ),
    );

    expect(find.text('주변 맛집이 없습니다'), findsOneWidget);
    expect(find.text('검색어나 위치를 바꿔 다시 탐색해보세요.'), findsOneWidget);
    expect(find.text('다시 찾기'), findsOneWidget);
    expect(find.byIcon(Icons.map_outlined), findsOneWidget);
  });
}
