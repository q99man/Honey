import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/theme/app_theme.dart';

void main() {
  group('AppTheme', () {
    test('builds a Material 3 light theme with Honeytong brand colors', () {
      final theme = AppTheme.light();

      expect(theme.useMaterial3, isTrue);
      expect(theme.colorScheme.primary, AppColors.honey);
      expect(theme.colorScheme.secondary, AppColors.nectar);
      expect(theme.scaffoldBackgroundColor, AppColors.background);
      expect(theme.cardTheme.color, AppColors.surface);
    });

    test('defines shared component shapes for mobile consistency', () {
      final theme = AppTheme.light();

      final buttonShape = theme.filledButtonTheme.style?.shape?.resolve({});
      final inputBorder = theme.inputDecorationTheme.border;

      expect(buttonShape, isA<RoundedRectangleBorder>());
      expect(inputBorder, isA<OutlineInputBorder>());
      expect(theme.snackBarTheme.behavior, SnackBarBehavior.floating);
      expect(theme.navigationBarTheme.height, 68);
    });
  });
}
