import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test('Home map starts real location lookup instead of staying on fallback',
      () {
    final source =
        File('lib/features/home/views/home_map_screen.dart').readAsStringSync();
    final initStateBody =
        RegExp(r'void initState\(\) \{([\s\S]*?)\n  \}').firstMatch(source)!;

    expect(initStateBody.group(1), contains('_determinePosition();'));
    expect(initStateBody.group(1),
        isNot(contains('_loadNearbyPlaces(fallbackPosition)')));
  });
}
