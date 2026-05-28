import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';

import '../../place/utils/place_category.dart';

class HomeMapMarkerAsset {
  const HomeMapMarkerAsset._();

  static Future<Uint8List> place({
    required String categoryCode,
    bool selected = false,
  }) {
    return _renderMarker(
      fillColor: PlaceCategory.markerColorFor(categoryCode),
      text: PlaceCategory.emojiFor(categoryCode),
      size: selected ? const Size(78, 90) : const Size(66, 76),
      selected: selected,
    );
  }

  static Future<Uint8List> currentLocation() {
    return _renderMarker(
      fillColor: const Color(0xFF1565C0),
      text: '⌖',
      size: const Size(58, 66),
    );
  }

  static Future<Uint8List> _renderMarker({
    required Color fillColor,
    required String text,
    required Size size,
    bool selected = false,
  }) async {
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);
    final paint = Paint()..isAntiAlias = true;
    final centerX = size.width / 2;
    final circleRadius = size.width * 0.35;
    final circleCenter = Offset(centerX, size.height * 0.35);

    final markerPath = Path()
      ..addOval(Rect.fromCircle(center: circleCenter, radius: circleRadius))
      ..moveTo(centerX - circleRadius * 0.52, size.height * 0.56)
      ..quadraticBezierTo(centerX, size.height * 0.98, centerX + 1, size.height)
      ..quadraticBezierTo(
        centerX,
        size.height * 0.98,
        centerX + circleRadius * 0.52,
        size.height * 0.56,
      )
      ..close();

    canvas.drawShadow(
      markerPath,
      Colors.black.withValues(alpha: selected ? 0.40 : 0.30),
      selected ? 8 : 5,
      true,
    );
    paint.color = fillColor;
    canvas.drawPath(markerPath, paint);

    paint
      ..style = PaintingStyle.stroke
      ..strokeWidth = selected ? 6 : 4
      ..color = Colors.white;
    canvas.drawPath(markerPath, paint);

    if (selected) {
      paint
        ..style = PaintingStyle.stroke
        ..strokeWidth = 4
        ..color = const Color(0xFFFFD54F).withValues(alpha: 0.72);
      canvas.drawCircle(circleCenter, circleRadius + 6, paint);
    }

    paint
      ..style = PaintingStyle.fill
      ..color = Colors.white.withValues(alpha: 0.92);
    canvas.drawCircle(circleCenter, circleRadius * 0.64, paint);

    paint
      ..style = PaintingStyle.fill
      ..color = Colors.white.withValues(alpha: 0.24);
    canvas.drawCircle(
      Offset(circleCenter.dx - circleRadius * 0.28, circleCenter.dy - 7),
      circleRadius * 0.38,
      paint,
    );

    final textPainter = TextPainter(
      text: TextSpan(
        text: text,
        style: TextStyle(
          color: Colors.black,
          fontSize: size.width * (selected ? 0.28 : 0.30),
          fontWeight: FontWeight.w900,
          height: 1,
        ),
      ),
      textDirection: TextDirection.ltr,
      maxLines: 1,
    )..layout(maxWidth: size.width);
    textPainter.paint(
      canvas,
      Offset(
        centerX - textPainter.width / 2,
        circleCenter.dy - textPainter.height / 2,
      ),
    );

    final picture = recorder.endRecording();
    final image = await picture.toImage(
      size.width.ceil(),
      size.height.ceil(),
    );
    final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
    picture.dispose();
    image.dispose();

    return bytes!.buffer.asUint8List();
  }
}
