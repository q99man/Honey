import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/theme/app_theme.dart';
import 'package:honeytong_mobile/core/widgets/app_section_title.dart';
import 'package:honeytong_mobile/core/widgets/app_surface_card.dart';

void main() {
  testWidgets('surface components render reusable Korean content',
      (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: AppTheme.light(),
        home: const Scaffold(
          body: Column(
            children: [
              AppSectionTitle('추천 정보'),
              AppSurfaceCard(
                child: Text('동네 사람들이 다시 찾는 맛집입니다.'),
              ),
            ],
          ),
        ),
      ),
    );

    expect(find.text('추천 정보'), findsOneWidget);
    expect(find.text('동네 사람들이 다시 찾는 맛집입니다.'), findsOneWidget);
  });
}
