import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/theme/app_theme.dart';
import 'package:honeytong_mobile/core/widgets/app_login_required_state.dart';

void main() {
  testWidgets('AppLoginRequiredState renders Korean login guidance',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: Scaffold(
          body: AppLoginRequiredState(
            icon: Icons.bookmark_add_rounded,
            title: '로그인이 필요합니다',
            description: '저장한 맛집을 보려면 로그인해주세요.',
            actionLabel: '로그인하러 가기',
            eyebrow: '나만의 맛집 보관함',
            previewItems: const [
              AppLoginPreviewItem(
                icon: Icons.bookmark_rounded,
                title: '추천한 맛집 자동 저장',
                description: '다시 가고 싶은 장소를 한 화면에서 확인합니다.',
              ),
            ],
            onActionPressed: () {},
          ),
        ),
      ),
    );

    expect(find.text('로그인이 필요합니다'), findsOneWidget);
    expect(find.text('저장한 맛집을 보려면 로그인해주세요.'), findsOneWidget);
    expect(find.text('로그인하러 가기'), findsOneWidget);
    expect(find.text('나만의 맛집 보관함'), findsOneWidget);
    expect(find.text('추천한 맛집 자동 저장'), findsOneWidget);
    expect(find.text('다시 가고 싶은 장소를 한 화면에서 확인합니다.'), findsOneWidget);
    expect(find.byIcon(Icons.bookmark_add_rounded), findsOneWidget);
    expect(find.byIcon(Icons.bookmark_rounded), findsOneWidget);
  });
}
