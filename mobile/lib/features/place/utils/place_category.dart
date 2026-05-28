import 'package:flutter/material.dart';

import '../../../utils/localization.dart';

class PlaceCategoryInfo {
  const PlaceCategoryInfo({
    required this.code,
    required this.translationKey,
    required this.markerColor,
    required this.emoji,
  });

  final String code;
  final String translationKey;
  final Color markerColor;
  final String emoji;

  String get label => translationKey.tr;
}

class PlaceCategory {
  const PlaceCategory._();

  static const defaultMarkerStyleId = 'honey_place_default';

  static const selectable = <PlaceCategoryInfo>[
    PlaceCategoryInfo(
      code: 'KOREAN',
      translationKey: 'home.category.korean',
      markerColor: Color(0xFFFF8F00),
      emoji: '🍚',
    ),
    PlaceCategoryInfo(
      code: 'CHINESE',
      translationKey: 'home.category.chinese',
      markerColor: Color(0xFFE53935),
      emoji: '🥢',
    ),
    PlaceCategoryInfo(
      code: 'JAPANESE',
      translationKey: 'home.category.japanese',
      markerColor: Color(0xFFD81B60),
      emoji: '🍣',
    ),
    PlaceCategoryInfo(
      code: 'WESTERN',
      translationKey: 'home.category.western',
      markerColor: Color(0xFF1E88E5),
      emoji: '🍝',
    ),
    PlaceCategoryInfo(
      code: 'SNACK',
      translationKey: 'home.category.snack',
      markerColor: Color(0xFF8E24AA),
      emoji: '🍢',
    ),
    PlaceCategoryInfo(
      code: 'CAFE',
      translationKey: 'home.category.cafe',
      markerColor: Color(0xFF6D4C41),
      emoji: '☕',
    ),
  ];

  static List<String> get selectableCodes =>
      selectable.map((category) => category.code).toList(growable: false);

  static bool contains(String code) => _byCode(code) != null;

  static String labelFor(String code) => _byCode(code)?.label ?? code;

  static Color markerColorFor(String code) =>
      _byCode(code)?.markerColor ?? const Color(0xFFFFB300);

  static String emojiFor(String code) => _byCode(code)?.emoji ?? '🍽️';

  static String markerStyleIdFor(String code, {bool selected = false}) {
    if (!contains(code)) {
      return selected
          ? '${defaultMarkerStyleId}_selected'
          : defaultMarkerStyleId;
    }
    return selected ? 'honey_place_${code}_selected' : 'honey_place_$code';
  }

  static String shortLabelFor(String code) {
    final label = labelFor(code);
    if (label.isEmpty || label == code) {
      return '맛';
    }
    return label.characters.first;
  }

  static PlaceCategoryInfo? _byCode(String code) {
    for (final category in selectable) {
      if (category.code == code) {
        return category;
      }
    }
    return null;
  }
}
