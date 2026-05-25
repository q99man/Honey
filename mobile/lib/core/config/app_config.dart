import 'package:flutter/services.dart';

class AppConfig {
  static const _configuredApiBaseUrl = String.fromEnvironment('HONEY_API_BASE_URL');
  static const _configuredDevApiBaseUrl = String.fromEnvironment('HONEY_DEV_API_BASE_URL');
  static const _configuredProdApiBaseUrl = String.fromEnvironment('HONEY_PROD_API_BASE_URL');
  static const kakaoNativeAppKey = String.fromEnvironment('KAKAO_NATIVE_APP_KEY');
  static const allowMockKakaoLogin = bool.fromEnvironment(
    'HONEY_ALLOW_MOCK_KAKAO_LOGIN',
    defaultValue: false,
  );

  static bool get isKakaoNativeConfigured => kakaoNativeAppKey.isNotEmpty;

  static String get apiBaseUrl {
    if (_configuredApiBaseUrl.isNotEmpty) {
      return _configuredApiBaseUrl;
    }

    if (appFlavor == 'prod') {
      if (_configuredProdApiBaseUrl.isNotEmpty) {
        return _configuredProdApiBaseUrl;
      }
      return 'https://api.honeytong.com';
    }

    if (_configuredDevApiBaseUrl.isNotEmpty) {
      return _configuredDevApiBaseUrl;
    }

    return 'http://10.0.2.2:8080';
  }
}
