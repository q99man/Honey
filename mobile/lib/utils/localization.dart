import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'translations.dart';

class Localization extends ChangeNotifier {
  static final Localization _instance = Localization._internal();
  factory Localization() => _instance;
  Localization._internal();

  String _locale = 'ko';
  String get locale => _locale;

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    _locale = prefs.getString('honeytong_locale') ?? 'ko';
    notifyListeners();
  }

  Future<void> changeLocale(String newLocale) async {
    if (newLocale == 'ko' || newLocale == 'en' || newLocale == 'ja') {
      _locale = newLocale;
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('honeytong_locale', newLocale);
      notifyListeners();
    }
  }

  static String translate(String key) {
    final currentLocale = _instance._locale;
    final map = Translations.keys[currentLocale] ?? Translations.keys['ko']!;
    return map[key] ?? key;
  }
}

extension TransExtension on String {
  String get tr => Localization.translate(this);
}
