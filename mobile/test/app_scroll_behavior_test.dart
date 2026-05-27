import 'dart:ui';

import 'package:flutter_test/flutter_test.dart';
import 'package:honeytong_mobile/core/widgets/app_scroll_behavior.dart';

void main() {
  test('AppScrollBehavior supports mouse drag for desktop carousel QA', () {
    const behavior = AppScrollBehavior();

    expect(behavior.dragDevices, contains(PointerDeviceKind.touch));
    expect(behavior.dragDevices, contains(PointerDeviceKind.mouse));
  });
}
